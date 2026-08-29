package vn.nitrogen.practice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một lần chấm bài.
 *
 * @param runType         AUTO | MANUAL | REGRADE
 * @param supersedesRunId run bị thay thế, null nếu là run đầu tiên
 */
public record GradingRunView(
        UUID id,
        UUID attemptId,
        int runNumber,
        String runType,
        String status,
        String graderCode,
        String graderVersion,
        UUID supersedesRunId,
        Instant startedAt,
        Instant completedAt,
        String errorCode) {
}
