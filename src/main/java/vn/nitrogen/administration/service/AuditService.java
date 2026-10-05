package vn.nitrogen.administration.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.administration.domain.AuditLog;
import vn.nitrogen.administration.repository.AuditLogRepository;
import vn.nitrogen.platform.observability.CorrelationId;

@Service
public class AuditService {

    private static final int MAX_LIMIT = 100;

    private final AuditLogRepository auditLogs;
    private final AuditRedactor redactor;

    public AuditService(AuditLogRepository auditLogs, AuditRedactor redactor) {
        this.auditLogs = auditLogs;
        this.redactor = redactor;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, String action, String targetType, UUID targetId, String reason) {
        AuditLog log = AuditLog.record(
                actorId,
                action,
                targetType,
                targetId,
                redactor.redact(reason),
                CorrelationId.currentOrNew(),
                Instant.now());

        auditLogs.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByTarget(String targetType, UUID targetId) {
        return auditLogs.findByTargetTypeAndTargetIdOrderByOccurredAtDesc(targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : Instant.now();
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);

        return auditLogs.findByActor(actorId, effectiveFrom, effectiveTo, PageRequest.of(0, safeLimit));
    }
}
