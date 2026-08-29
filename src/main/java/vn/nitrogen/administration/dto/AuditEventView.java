package vn.nitrogen.administration.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một dòng audit log.
 *
 * @param actorId    người thực hiện, null nếu là hệ thống
 * @param targetType loại đối tượng bị tác động, ví dụ {@code PRACTICE_ATTEMPT}
 */
public record AuditEventView(
        UUID id,
        UUID actorId,
        String action,
        String targetType,
        UUID targetId,
        Instant occurredAt,
        String reason) {
}
