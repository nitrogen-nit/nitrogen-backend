package vn.nitrogen.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.domain.OutboxStatus;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.dto.ClaimedOutboxEvent;
import vn.nitrogen.integration.dto.OutboxEventView;
import vn.nitrogen.integration.repository.OutboxEventRepository;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
class OutboxApiTest extends TestcontainersBase {

    @Autowired
    private OutboxApi outbox;

    @Autowired
    private OutboxEventRepository outboxEvents;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanOutbox() {
        outboxEvents.deleteAllInBatch();
    }

    @Test
    void appendRequiresExistingBusinessTransaction() {
        AppendOutboxCommand command = command();

        assertThatThrownBy(() -> outbox.append(command))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void appendPersistsPendingEventAndJsonPayload() {
        AppendOutboxCommand command = command();

        UUID id = transactionTemplate().execute(status -> outbox.append(command));

        OutboxEvent event = outboxEvents.findById(id).orElseThrow();
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getAggregateType()).isEqualTo(command.aggregateType());
        assertThat(event.getAggregateId()).isEqualTo(command.aggregateId());
        assertThat(event.getEventType()).isEqualTo(command.eventType());
        assertThat(event.getRoutingKey()).isEqualTo(command.routingKey());
        assertThat(event.getSchemaVersion()).isEqualTo(command.schemaVersion());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getPayload()).containsEntry("email", "user@example.com");
        assertThat(event.getCorrelationId()).isEqualTo(command.correlationId());
        assertThat(event.getCausationId()).isEqualTo(command.causationId());
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isEqualTo(event.getOccurredAt());
    }

    @Test
    void appendRollsBackWithBusinessTransactionAfterFlush() {
        AtomicReference<UUID> eventId = new AtomicReference<>();

        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
                    eventId.set(outbox.append(command()));
                    outboxEvents.flush();
                    throw new IllegalStateException("business failure");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");

        assertThat(eventId.get()).isNotNull();
        assertThat(outboxEvents.findById(eventId.get())).isEmpty();
    }

    @Test
    void findFailedReturnsOnlyFailedEventsAndHonorsLimit() {
        UUID olderFailedId = appendInTransaction();
        UUID newerFailedId = appendInTransaction();
        UUID pendingId = appendInTransaction();

        markFailed(olderFailedId, "FIRST_ERROR");
        markFailed(newerFailedId, "BROKER_TIMEOUT");

        List<OutboxEventView> failed = outbox.findFailed(1);

        assertThat(failed).hasSize(1);
        assertThat(failed)
                .extracting(OutboxEventView::id)
                .contains(newerFailedId)
                .doesNotContain(olderFailedId, pendingId);
        assertThat(failed.getFirst().status()).isEqualTo("FAILED");
        assertThat(failed.getFirst().lastErrorCode()).isEqualTo("BROKER_TIMEOUT");
    }

    @Test
    void findFailedClampsNonPositiveLimitToOne() {
        UUID failedId = appendInTransaction();
        markFailed(failedId, "BROKER_TIMEOUT");

        assertThat(outbox.findFailed(0))
                .singleElement()
                .extracting(OutboxEventView::id)
                .isEqualTo(failedId);
    }

    @Test
    void differentWorkersDoNotClaimTheSameEvent() {
        UUID firstId = appendInTransaction();
        UUID secondId = appendInTransaction();
        Instant now = databaseNow().plusSeconds(1);

        List<ClaimedOutboxEvent> firstClaim =
                outbox.claimPending("worker-a", now, Duration.ofMinutes(1), 1);
        List<ClaimedOutboxEvent> secondClaim =
                outbox.claimPending("worker-b", now, Duration.ofMinutes(1), 10);

        assertThat(firstClaim).hasSize(1);
        assertThat(secondClaim).hasSize(1);
        assertThat(firstClaim.getFirst().id()).isIn(firstId, secondId);
        assertThat(secondClaim.getFirst().id()).isIn(firstId, secondId);
        assertThat(secondClaim.getFirst().id()).isNotEqualTo(firstClaim.getFirst().id());
    }

    @Test
    void expiredLeaseCanBeReclaimed() {
        UUID eventId = appendInTransaction();
        Instant firstClaimTime = databaseNow().plusSeconds(1);

        assertThat(outbox.claimPending("dead-worker", firstClaimTime, Duration.ofSeconds(5), 1))
                .extracting(ClaimedOutboxEvent::id)
                .containsExactly(eventId);

        assertThat(outbox.claimPending(
                        "replacement-worker",
                        firstClaimTime.plusSeconds(6),
                        Duration.ofSeconds(5),
                        1))
                .extracting(ClaimedOutboxEvent::id)
                .containsExactly(eventId);
    }

    @Test
    void onlyLeaseOwnerCanMarkEventPublished() {
        UUID eventId = appendInTransaction();
        Instant now = databaseNow().plusSeconds(1);
        outbox.claimPending("owner", now, Duration.ofMinutes(1), 1);

        assertThatThrownBy(() -> outbox.markPublished(eventId, "other-worker", now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(eventId.toString());

        outbox.markPublished(eventId, "owner", now);
        OutboxEvent event = outboxEvents.findById(eventId).orElseThrow();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(now);
        assertThat(event.getLockedBy()).isNull();
        assertThat(event.getLockedUntil()).isNull();
    }

    @Test
    void ownerCanRescheduleAndEventuallyMarkEventFailed() {
        UUID eventId = appendInTransaction();
        Instant now = databaseNow().plusSeconds(1);
        outbox.claimPending("worker", now, Duration.ofMinutes(1), 1);

        Instant nextRetry = now.plusSeconds(10);
        outbox.reschedule(eventId, "worker", 1, nextRetry, "BROKER_DOWN", "connection refused");

        OutboxEvent rescheduled = outboxEvents.findById(eventId).orElseThrow();
        assertThat(rescheduled.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(rescheduled.getRetryCount()).isEqualTo(1);
        assertThat(rescheduled.getNextRetryAt()).isEqualTo(nextRetry);
        assertThat(rescheduled.getLastErrorCode()).isEqualTo("BROKER_DOWN");
        assertThat(rescheduled.getLockedBy()).isNull();

        outbox.claimPending("worker", nextRetry.plusSeconds(1), Duration.ofMinutes(1), 1);
        Instant failedAt = nextRetry.plusSeconds(2);
        outbox.markFailed(eventId, "worker", 2, failedAt, "BROKER_DOWN", "still unavailable");

        OutboxEvent failed = outboxEvents.findById(eventId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(2);
        assertThat(failed.getFailedAt()).isEqualTo(failedAt);
        assertThat(failed.getLockedBy()).isNull();
    }

    private UUID appendInTransaction() {
        return transactionTemplate().execute(status -> outbox.append(command()));
    }

    private void markFailed(UUID id, String errorCode) {
        jdbcTemplate.update(
                """
                UPDATE integration.outbox_events
                SET status = 'FAILED',
                    failed_at = now(),
                    last_error_code = ?,
                    last_error = 'timed out'
                WHERE id = ?
                """,
                errorCode,
                id);
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    // PostgreSQL stores microseconds and pgjdbc rounds extra nanos, so tests start at that precision.
    private static Instant databaseNow() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static AppendOutboxCommand command() {
        return new AppendOutboxCommand(
                "USER",
                UUID.randomUUID(),
                "UserRegistered",
                "identity.user.registered.v1",
                1,
                UUID.randomUUID(),
                null,
                Map.of("email", "user@example.com"));
    }
}
