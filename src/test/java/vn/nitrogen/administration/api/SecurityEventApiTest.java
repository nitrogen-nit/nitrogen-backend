package vn.nitrogen.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.nitrogen.administration.domain.SecurityEventOutcome;
import vn.nitrogen.administration.domain.SecurityEventSeverity;
import vn.nitrogen.administration.domain.SecurityEventType;
import vn.nitrogen.administration.dto.SecurityEventView;
import vn.nitrogen.platform.observability.CorrelationId;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
class SecurityEventApiTest extends TestcontainersBase {

    @Autowired
    private SecurityEventApi securityEvents;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordPersistsSecurityEventWithCorrelationIdAndSanitizedFields() {
        UUID actorId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        securityEvents.record(
                actorId,
                SecurityEventType.LOGIN_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                " 127.0.0.1 ",
                "Mozilla\nInjected",
                "password=plain token=abc bad credentials");

        SecurityEventView event = only(securityEvents.findByCorrelationId(correlationId));

        assertThat(event.actorId()).isEqualTo(actorId);
        assertThat(event.eventType()).isEqualTo(SecurityEventType.LOGIN_FAILURE);
        assertThat(event.severity()).isEqualTo(SecurityEventSeverity.WARNING);
        assertThat(event.outcome()).isEqualTo(SecurityEventOutcome.FAILURE);
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(event.userAgent()).isEqualTo("MozillaInjected");
        assertThat(event.reason())
                .contains("password=[REDACTED]", "token=[REDACTED]")
                .doesNotContain("plain", "abc");
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void recordCommitsEvenWhenOuterTransactionRollsBack() {
        UUID actorId = UUID.randomUUID();
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            securityEvents.record(
                    actorId,
                    SecurityEventType.UNAUTHORIZED_ACCESS,
                    SecurityEventSeverity.CRITICAL,
                    SecurityEventOutcome.BLOCKED,
                    "127.0.0.1",
                    "Mozilla/5.0",
                    "unauthorized");
            throw new IllegalStateException("business rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(securityEvents.findByCorrelationId(correlationId))
                .extracting(SecurityEventView::eventType)
                .containsExactly(SecurityEventType.UNAUTHORIZED_ACCESS);
    }

    @Test
    void findByActorReturnsNewestFirstAndUsesLimit() {
        UUID actorId = UUID.randomUUID();

        securityEvents.record(
                actorId,
                SecurityEventType.LOGIN_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                "127.0.0.1",
                "Mozilla/5.0",
                "first");

        securityEvents.record(
                actorId,
                SecurityEventType.RATE_LIMITED,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.BLOCKED,
                "127.0.0.1",
                "Mozilla/5.0",
                "second");

        assertThat(securityEvents.findByActor(actorId, null, null, 1))
                .extracting(SecurityEventView::eventType)
                .containsExactly(SecurityEventType.RATE_LIMITED);
    }

    @Test
    void findByEventTypeReturnsNewestFirstAndUsesLimit() {
        securityEvents.record(
                UUID.randomUUID(),
                SecurityEventType.LOGIN_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                "127.0.0.1",
                "Mozilla/5.0",
                "first");

        securityEvents.record(
                UUID.randomUUID(),
                SecurityEventType.LOGIN_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                "127.0.0.2",
                "Mozilla/5.0",
                "second");

        assertThat(securityEvents.findByEventType(SecurityEventType.LOGIN_FAILURE, 1))
                .extracting(SecurityEventView::ipAddress)
                .containsExactly("127.0.0.2");
    }

    private static SecurityEventView only(List<SecurityEventView> events) {
        assertThat(events).hasSize(1);
        return events.getFirst();
    }
}
