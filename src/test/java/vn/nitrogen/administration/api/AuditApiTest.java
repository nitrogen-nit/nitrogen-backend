package vn.nitrogen.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.nitrogen.administration.dto.AuditEventView;
import vn.nitrogen.platform.observability.CorrelationId;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
class AuditApiTest extends TestcontainersBase {

    @Autowired
    private AuditApi audit;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordRequiresExistingBusinessTransaction() {
        assertThatThrownBy(() -> audit.record(
                UUID.randomUUID(),
                "USER_REGISTERED",
                "USER",
                UUID.randomUUID(),
                "registration accepted"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void recordPersistsAuditLogWithCorrelationIdAndRedactedReason() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        recordInTransaction(actorId, "USER_REGISTERED", "USER", targetId,
                "password=plain token=abc secret=hidden registration accepted");

        AuditEventView event = only(audit.findByTarget("USER", targetId));
        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.action()).isEqualTo("USER_REGISTERED");
        assertThat(event.targetType()).isEqualTo("USER");
        assertThat(event.targetId()).isEqualTo(targetId);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.reason())
                .contains("password=[REDACTED]", "token=[REDACTED]", "secret=[REDACTED]")
                .doesNotContain("plain", "abc", "hidden");
    }

    @Test
    void findByTargetReturnsNewestFirst() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        recordInTransaction(actorId, "FIRST_ACTION", "USER", targetId, "first");
        recordInTransaction(actorId, "SECOND_ACTION", "USER", targetId, "second");

        assertThat(audit.findByTarget("USER", targetId))
                .extracting(AuditEventView::action)
                .containsExactly("SECOND_ACTION", "FIRST_ACTION");
    }

    @Test
    void findByActorFiltersByRangeAndLimit() {
        UUID actorId = UUID.randomUUID();
        UUID otherActorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant from = Instant.now().minusSeconds(60);

        recordInTransaction(actorId, "FIRST_ACTION", "USER", targetId, "first");
        recordInTransaction(actorId, "SECOND_ACTION", "USER", UUID.randomUUID(), "second");
        recordInTransaction(otherActorId, "OTHER_ACTION", "USER", UUID.randomUUID(), "other");

        Instant to = Instant.now().plusSeconds(60);

        assertThat(audit.findByActor(actorId, from, to, 1))
                .extracting(AuditEventView::action)
                .containsExactly("SECOND_ACTION");
    }

    @Test
    void findByActorUsesDefaultRangeAndMinimumLimit() {
        UUID actorId = UUID.randomUUID();

        recordInTransaction(actorId, "DEFAULT_RANGE_ACTION", "USER", UUID.randomUUID(), "default range");

        assertThat(audit.findByActor(actorId, null, null, 0))
                .extracting(AuditEventView::action)
                .containsExactly("DEFAULT_RANGE_ACTION");
    }

    @Test
    void recordRollsBackWithBusinessTransaction() {
        UUID targetId = UUID.randomUUID();

        assertThatThrownBy(() -> newTransactionTemplate().executeWithoutResult(status -> {
            audit.record(UUID.randomUUID(), "ROLLBACK_ACTION", "USER", targetId, "rolled back");
            throw new IllegalStateException("business failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(audit.findByTarget("USER", targetId)).isEmpty();
    }

    private void recordInTransaction(UUID actorId, String action, String targetType, UUID targetId, String reason) {
        newTransactionTemplate().executeWithoutResult(status ->
                audit.record(actorId, action, targetType, targetId, reason));
    }

    private TransactionTemplate newTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private static AuditEventView only(List<AuditEventView> events) {
        assertThat(events).hasSize(1);
        return events.getFirst();
    }
}
