package vn.nitrogen.integration.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một bản ghi outbox, dùng cho màn hình vận hành và alert.
 *
 * <p>Cố tình không có {@code payload}: outbox payload có thể chứa dữ liệu bài
 * làm, không nên đi ra ngoài Integration chỉ để hiển thị trạng thái.
 *
 * @param status PENDING | PUBLISHED | FAILED
 */
public record OutboxEventView(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        int schemaVersion,
        String status,
        int retryCount,
        Instant occurredAt,
        Instant nextRetryAt,
        Instant publishedAt,
        String lastErrorCode) {
}
