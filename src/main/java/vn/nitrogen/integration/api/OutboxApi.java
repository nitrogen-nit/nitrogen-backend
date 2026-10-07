package vn.nitrogen.integration.api;

import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.dto.OutboxEventView;
import vn.nitrogen.integration.service.OutboxService;

/**
 * Ghi event vào outbox và đọc trạng thái outbox.
 *
 * <p>{@link #append} phải được gọi TRONG transaction nghiệp vụ của module gọi —
 * đó là toàn bộ lý do outbox tồn tại: event và thay đổi dữ liệu commit cùng
 * nhau. Việc publish sang RabbitMQ diễn ra sau, ngoài transaction (§4.3: không
 * gọi RabbitMQ trong transaction).
 */
@Profile("core")
@Controller
@Lazy
public class OutboxApi implements ModuleApi {

    private final OutboxService outbox;

    public OutboxApi(OutboxService outbox) {
        this.outbox = outbox;
    }

    public UUID append(AppendOutboxCommand command) {
        return outbox.append(command);
    }

    public List<OutboxEventView> findFailed(int limit) {
        return outbox.findFailed(limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    private OutboxEventView toView(OutboxEvent event) {
        return new OutboxEventView(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getSchemaVersion(),
                event.getStatus().name(),
                event.getRetryCount(),
                event.getOccurredAt(),
                event.getNextRetryAt(),
                event.getPublishedAt(),
                event.getLastErrorCode());
    }
}
