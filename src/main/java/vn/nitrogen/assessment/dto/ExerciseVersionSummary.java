package vn.nitrogen.assessment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ảnh chụp bất biến của một version câu hỏi đã PUBLISHED.
 *
 * <p>Practice snapshot {@code exerciseVersionId} vào {@code attempt_items} tại
 * thời điểm bắt đầu làm bài; sửa nội dung sau đó tạo version mới và không
 * ảnh hưởng bài đã làm (§13.1).
 *
 * @param maxScore điểm tối đa, NUMERIC/BigDecimal — không dùng double (§7)
 */
public record ExerciseVersionSummary(
        UUID id,
        UUID exerciseId,
        int versionNumber,
        String questionType,
        BigDecimal maxScore,
        UUID topicNodeId) {
}
