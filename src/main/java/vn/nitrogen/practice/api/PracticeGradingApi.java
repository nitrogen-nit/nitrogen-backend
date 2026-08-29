package vn.nitrogen.practice.api;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.practice.dto.GradingRunView;

/**
 * Bề mặt điều khiển việc chấm bài mà module khác được phép chạm tới:
 * Administration yêu cầu chấm lại, worker báo cáo kết quả.
 *
 * <p>Đây là facade duy nhất trong Practice có method ghi. Nó cố tình KHÔNG nhận
 * điểm số từ bên ngoài — chỉ nhận yêu cầu, còn việc chấm và ghi điểm nằm trong
 * service nội bộ.
 *
 * <p>TODO: inject {@code GradingRunService} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class PracticeGradingApi implements ModuleApi {

    public List<GradingRunView> findRunsByAttempt(UUID attemptId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực PracticeGradingApi#findRunsByAttempt");
    }

    /**
     * Xếp hàng một lần chấm lại.
     *
     * @param requestedBy user thực hiện, để ghi audit
     * @return id của grading run mới
     */
    public UUID requestRegrade(UUID attemptId, UUID requestedBy, String reason) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực PracticeGradingApi#requestRegrade");
    }
}
