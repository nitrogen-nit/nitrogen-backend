package vn.nitrogen.administration.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.administration.domain.SecurityEvent;
import vn.nitrogen.administration.domain.SecurityEventOutcome;
import vn.nitrogen.administration.domain.SecurityEventSeverity;
import vn.nitrogen.administration.domain.SecurityEventType;
import vn.nitrogen.administration.repository.SecurityEventRepository;
import vn.nitrogen.platform.observability.CorrelationId;

@Service
public class SecurityEventService {

    private static final int MAX_LIMIT = 100;

    private final SecurityEventRepository securityEvents;
    private final SecurityEventSanitizer sanitizer;

    public SecurityEventService(
            SecurityEventRepository securityEvents,
            SecurityEventSanitizer sanitizer) {
        this.securityEvents = securityEvents;
        this.sanitizer = sanitizer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID actorId,
            SecurityEventType eventType,
            SecurityEventSeverity severity,
            SecurityEventOutcome outcome,
            String ipAddress,
            String userAgent,
            String reason) {
        SecurityEvent event = SecurityEvent.record(
                actorId,
                eventType,
                severity,
                outcome,
                sanitizer.sanitizeIpAddress(ipAddress),
                sanitizer.sanitizeUserAgent(userAgent),
                sanitizer.sanitizeReason(reason),
                CorrelationId.currentOrNew(),
                Instant.now());

        securityEvents.save(event);
    }

    @Transactional(readOnly = true)
    public List<SecurityEvent> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);

        return securityEvents.findByActor(
                actorId,
                effectiveFrom,
                effectiveTo,
                PageRequest.of(0, safeLimit));
    }

    @Transactional(readOnly = true)
    public List<SecurityEvent> findByCorrelationId(String correlationId) {
        return securityEvents.findByCorrelationIdOrderByOccurredAtDesc(correlationId);
    }

    @Transactional(readOnly = true)
    public List<SecurityEvent> findByEventType(SecurityEventType eventType, int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        return securityEvents.findByEventTypeOrderByOccurredAtDesc(
                eventType,
                PageRequest.of(0, safeLimit));
    }
}
