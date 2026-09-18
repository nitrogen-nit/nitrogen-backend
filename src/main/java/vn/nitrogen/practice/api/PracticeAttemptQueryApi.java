package vn.nitrogen.practice.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.practice.dto.PracticeAttemptSummary;
import vn.nitrogen.practice.service.PracticeAttemptService;

/**
 * Đọc trạng thái attempt cho module khác.
 *
 * <p>Chỉ đọc — không có method nào đổi trạng thái. Mọi chuyển trạng thái attempt
 * đều phải đi qua service nội bộ của Practice để state machine chỉ có một chỗ
 * thực thi.
 *
 * <p>Facade mỏng qua service để cách dựng read model không bị nhân đôi giữa REST
 * và cross-module API.
 */
@Profile("core")
@Controller
public class PracticeAttemptQueryApi implements ModuleApi {

    private final PracticeAttemptService attempts;

    public PracticeAttemptQueryApi(PracticeAttemptService attempts) {
        this.attempts = attempts;
    }

    public Optional<PracticeAttemptSummary> findById(UUID attemptId) {
        return attempts.findOptionalById(attemptId);
    }

    public List<PracticeAttemptSummary> findRecentByUser(UUID userId, int limit) {
        return attempts.findRecentByUser(userId, limit);
    }

    public long countByUserAndOrigin(UUID userId, String originType, UUID originId) {
        return attempts.countByUserAndOrigin(userId, originType, originId);
    }
}
