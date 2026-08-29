package vn.nitrogen.practice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái một lượt làm bài, đủ cho Progress và Administration.
 *
 * @param status        IN_PROGRESS | SUBMITTED | COMPLETED | EXPIRED | CANCELLED
 * @param gradingStatus NOT_REQUIRED | PENDING_AUTO | PENDING_MANUAL |
 *                      PARTIALLY_GRADED | GRADED | FAILED
 * @param scoreFinal    true khi điểm đã chốt, không còn grading run nào chạy
 */
public record PracticeAttemptSummary(
        UUID id,
        UUID userId,
        String attemptKind,
        String originType,
        UUID originId,
        int attemptNo,
        String status,
        String gradingStatus,
        BigDecimal score,
        BigDecimal maxScore,
        boolean scoreFinal,
        Instant startedAt,
        Instant submittedAt,
        Instant completedAt) {
}
