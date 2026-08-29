package vn.nitrogen.assessment.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Đáp án đúng của một version câu hỏi.
 *
 * <p>BACKEND-ONLY. Không bao giờ được đưa vào response của learner và không
 * được log trong learner request log (§13.3). Chỉ grader và luồng review đọc.
 *
 * @param spec payload JSONB khớp contracts/json-schema/answer-spec
 */
public record AnswerSpecView(UUID exerciseVersionId, int schemaVersion, Map<String, Object> spec) {
}
