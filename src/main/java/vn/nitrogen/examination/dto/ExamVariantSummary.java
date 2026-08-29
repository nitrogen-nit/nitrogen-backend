package vn.nitrogen.examination.dto;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Một biến thể đề thi đã chốt, đủ để Practice dựng attempt.
 *
 * @param exerciseVersionIds theo đúng thứ tự hiển thị; Practice snapshot danh
 *                           sách này vào attempt_items
 */
public record ExamVariantSummary(
        UUID id,
        UUID examId,
        String variantCode,
        Duration timeLimit,
        List<UUID> exerciseVersionIds) {
}
