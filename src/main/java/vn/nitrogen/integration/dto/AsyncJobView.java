package vn.nitrogen.integration.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái một job bất đồng bộ (chấm bài, xuất PDF, rebuild progress).
 */
public record AsyncJobView(
        UUID id,
        String jobType,
        String status,
        int attemptCount,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String lastErrorCode) {
}
