package vn.nitrogen.examination.api;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.examination.dto.ExamVariantSummary;

/**
 * Cấp biến thể đề thi cho Practice khi bắt đầu một attempt có
 * {@code origin_type = EXAM_VARIANT}.
 *
 * <p>TODO: inject {@code ExamVariantRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class ExamVariantApi implements ModuleApi {

    public Optional<ExamVariantSummary> findById(UUID variantId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực ExamVariantApi#findById");
    }

    /** Chọn biến thể cho một user — luật chống trùng đề nằm trong Examination. */
    public Optional<ExamVariantSummary> pickVariantForUser(UUID examId, UUID userId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực ExamVariantApi#pickVariantForUser");
    }
}
