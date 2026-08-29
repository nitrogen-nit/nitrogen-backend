package vn.nitrogen.practice.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.practice.dto.PracticeAttemptSummary;

/**
 * Đọc trạng thái attempt cho module khác.
 *
 * <p>Chỉ đọc — không có method nào đổi trạng thái. Mọi chuyển trạng thái attempt
 * đều phải đi qua service nội bộ của Practice để state machine chỉ có một chỗ
 * thực thi.
 *
 * <p>TODO: inject {@code PracticeAttemptRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class PracticeAttemptQueryApi implements ModuleApi {

    public Optional<PracticeAttemptSummary> findById(UUID attemptId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực PracticeAttemptQueryApi#findById");
    }

    public List<PracticeAttemptSummary> findRecentByUser(UUID userId, int limit) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực PracticeAttemptQueryApi#findRecentByUser");
    }

    public long countByUserAndOrigin(UUID userId, String originType, UUID originId) {
        throw new UnsupportedOperationException(
                "TODO: chưa hiện thực PracticeAttemptQueryApi#countByUserAndOrigin");
    }
}
