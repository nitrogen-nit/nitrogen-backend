package vn.nitrogen.administration.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.administration.dto.AuditEventView;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Ghi và tra cứu audit.
 *
 * <p>Mọi module đều được ghi audit; chỉ Administration được đọc. Ghi phải nằm
 * trong cùng transaction với hành động được audit — audit rơi mất thì bản ghi
 * mất giá trị pháp lý.
 *
 * <p>TODO: inject {@code AuditLogRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class AuditApi implements ModuleApi {

    public void record(UUID actorId, String action, String targetType, UUID targetId, String reason) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực AuditApi#record");
    }

    public List<AuditEventView> findByTarget(String targetType, UUID targetId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực AuditApi#findByTarget");
    }

    public List<AuditEventView> findByActor(UUID actorId, Instant from, Instant to, int limit) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực AuditApi#findByActor");
    }
}
