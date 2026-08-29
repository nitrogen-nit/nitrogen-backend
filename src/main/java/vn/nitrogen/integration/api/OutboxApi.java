package vn.nitrogen.integration.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.integration.dto.OutboxEventView;

/**
 * Ghi event vào outbox và đọc trạng thái outbox.
 *
 * <p>{@link #append} phải được gọi TRONG transaction nghiệp vụ của module gọi —
 * đó là toàn bộ lý do outbox tồn tại: event và thay đổi dữ liệu commit cùng
 * nhau. Việc publish sang RabbitMQ diễn ra sau, ngoài transaction (§4.3: không
 * gọi RabbitMQ trong transaction).
 *
 * <p>TODO: inject {@code OutboxEventRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class OutboxApi implements ModuleApi {

    /**
     * Ghi một event chờ publish.
     *
     * @return id của bản ghi outbox (UUIDv7)
     */
    public UUID append(String aggregateType, UUID aggregateId, String eventType,
            int schemaVersion, Map<String, Object> payload) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực OutboxApi#append");
    }

    public List<OutboxEventView> findFailed(int limit) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực OutboxApi#findFailed");
    }
}
