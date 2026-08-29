package vn.nitrogen.practice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả theo chủ đề của một attempt — nguồn để Progress dựng projection.
 *
 * <p>Gắn với {@code gradingRunId} chứ không chỉ {@code attemptId}: một attempt
 * có thể được chấm lại, và projection phải biết mình đang dựng từ run nào để
 * regrade không cộng dồn hai lần.
 */
public record AttemptTopicResultView(
        UUID attemptId,
        UUID topicNodeId,
        UUID gradingRunId,
        int calculationVersion,
        BigDecimal earnedScore,
        BigDecimal maxScore,
        BigDecimal weight,
        int correctCount,
        int totalCount,
        int durationSeconds) {
}
