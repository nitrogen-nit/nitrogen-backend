package vn.nitrogen.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
