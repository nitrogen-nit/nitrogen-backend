package vn.nitrogen.assessment.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.assessment.dto.AnswerSpecView;
import vn.nitrogen.common.api.ModuleApi;

/**
 * Cấp đáp án đúng cho grader.
 *
 * <p>Tách thành facade riêng vì đây là dữ liệu nhạy cảm nhất hệ thống: chỉ
 * grader được gọi, và tách ra thì luật phân quyền cùng audit chỉ phải bảo vệ
 * một bề mặt duy nhất thay vì rải trong một God-API.
 *
 * <p>TODO: inject {@code AnswerSpecRepository} nội bộ module, thêm kiểm tra
 * caller và audit mỗi lần đọc.
 */
@Profile("core")
@Controller
@Lazy
public class AnswerSpecApi implements ModuleApi {

    public Optional<AnswerSpecView> findByExerciseVersionId(UUID exerciseVersionId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực AnswerSpecApi#findByExerciseVersionId");
    }

    public List<AnswerSpecView> findAllByExerciseVersionId(Collection<UUID> exerciseVersionIds) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực AnswerSpecApi#findAllByExerciseVersionId");
    }
}
