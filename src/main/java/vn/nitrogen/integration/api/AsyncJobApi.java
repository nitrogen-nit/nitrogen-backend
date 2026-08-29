package vn.nitrogen.integration.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.integration.dto.AsyncJobView;

/**
 * Xếp hàng và theo dõi job bất đồng bộ.
 *
 * <p>Tách khỏi {@link OutboxApi} vì hai thứ khác nhau: outbox là "đã xảy ra,
 * hãy loan báo", async job là "hãy làm việc này". Gộp lại thì retry policy và
 * ngữ nghĩa idempotency của cả hai đều bị nhoè.
 *
 * <p>TODO: inject {@code AsyncJobRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class AsyncJobApi implements ModuleApi {

    /**
     * Xếp hàng một job.
     *
     * @param idempotencyKey khoá chống trùng; gọi lại cùng khoá trả về job cũ
     */
    public UUID enqueue(String jobType, Map<String, Object> payload, String idempotencyKey) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực AsyncJobApi#enqueue");
    }

    public Optional<AsyncJobView> findById(UUID jobId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực AsyncJobApi#findById");
    }
}
