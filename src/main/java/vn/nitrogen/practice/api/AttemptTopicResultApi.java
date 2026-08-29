package vn.nitrogen.practice.api;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.practice.dto.AttemptTopicResultView;

/**
 * Cấp kết quả theo chủ đề cho Progress dựng projection và rebuild.
 *
 * <p>Tách khỏi {@link PracticeAttemptQueryApi} vì đây là đường đọc hàng loạt
 * của job rebuild, có đặc tính truy cập (phân trang theo khoảng thời gian,
 * batch lớn) hoàn toàn khác đường đọc một attempt.
 *
 * <p>TODO: inject {@code AttemptTopicResultRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class AttemptTopicResultApi implements ModuleApi {

    public List<AttemptTopicResultView> findByAttemptAndGradingRun(UUID attemptId, UUID gradingRunId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực AttemptTopicResultApi#findByAttemptAndGradingRun");
    }

    /** Đọc theo trang cho job rebuild progress — không nạp hết vào bộ nhớ. */
    public List<AttemptTopicResultView> findPageForRebuild(UUID userId, UUID afterAttemptId, int limit) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực AttemptTopicResultApi#findPageForRebuild");
    }
}
