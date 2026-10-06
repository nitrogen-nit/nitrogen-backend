package vn.nitrogen.administration.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.administration.domain.SecurityEvent;
import vn.nitrogen.administration.domain.SecurityEventOutcome;
import vn.nitrogen.administration.domain.SecurityEventSeverity;
import vn.nitrogen.administration.domain.SecurityEventType;
import vn.nitrogen.administration.dto.SecurityEventView;
import vn.nitrogen.administration.service.SecurityEventService;
import vn.nitrogen.common.api.ModuleApi;

@Profile("core")
@Controller
@Lazy
public class SecurityEventApi implements ModuleApi {

    private final SecurityEventService securityEvents;

    public SecurityEventApi(SecurityEventService securityEvents) {
        this.securityEvents = securityEvents;
    }

    public void record(
            UUID actorId,
            SecurityEventType eventType,
            SecurityEventSeverity severity,
            SecurityEventOutcome outcome,
            String ipAddress,
            String userAgent,
            String reason) {
        securityEvents.record(
                actorId,
                eventType,
                severity,
                outcome,
                ipAddress,
                userAgent,
                reason);
    }

    public List<SecurityEventView> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        return securityEvents.findByActor(actorId, from, to, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<SecurityEventView> findByCorrelationId(String correlationId) {
        return securityEvents.findByCorrelationId(correlationId)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<SecurityEventView> findByEventType(SecurityEventType eventType, int limit) {
        return securityEvents.findByEventType(eventType, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    private SecurityEventView toView(SecurityEvent event) {
        return new SecurityEventView(
                event.getId(),
                event.getActorId(),
                event.getEventType(),
                event.getSeverity(),
                event.getOutcome(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getReason(),
                event.getCorrelationId(),
                event.getOccurredAt());
    }
}
