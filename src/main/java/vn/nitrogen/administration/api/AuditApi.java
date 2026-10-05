package vn.nitrogen.administration.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.administration.domain.AuditLog;
import vn.nitrogen.administration.dto.AuditEventView;
import vn.nitrogen.administration.service.AuditService;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Ghi và tra cứu audit.
 *
 * <p>Mọi module đều được ghi audit; chỉ Administration được đọc. Ghi phải nằm
 * trong cùng transaction với hành động được audit — audit rơi mất thì bản ghi
 * mất giá trị pháp lý.
 */
@Profile("core")
@Controller
@Lazy
public class AuditApi implements ModuleApi {

    private final AuditService audit;

    public AuditApi(AuditService audit) {
        this.audit = audit;
    }

    public void record(UUID actorId, String action, String targetType, UUID targetId, String reason) {
        audit.record(actorId, action, targetType, targetId, reason);
    }

    public List<AuditEventView> findByTarget(String targetType, UUID targetId) {
        return audit.findByTarget(targetType, targetId)
                .stream()
                .map(this::toView)
                .toList();
    }

    public List<AuditEventView> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        return audit.findByActor(actorId, from, to, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    private AuditEventView toView(AuditLog log) {
        return new AuditEventView(
                log.getId(),
                log.getActorId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getOccurredAt(),
                log.getReason(),
                log.getCorrelationId());
    }
}
