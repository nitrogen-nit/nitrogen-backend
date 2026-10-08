package vn.nitrogen.integration.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.dto.ClaimedOutboxEvent;
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

    public List<ClaimedOutboxEvent> claimPending(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int batchSize) {
        return outbox.claimPending(workerId, now, leaseDuration, batchSize);
    }

    public void markPublished(UUID eventId, String workerId, Instant publishedAt) {
        outbox.markPublished(eventId, workerId, publishedAt);
    }

    public void reschedule(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant nextRetryAt,
            String errorCode,
            String error) {
        outbox.reschedule(eventId, workerId, retryCount, nextRetryAt, errorCode, error);
    }

    public void markFailed(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant failedAt,
            String errorCode,
            String error) {
        outbox.markFailed(eventId, workerId, retryCount, failedAt, errorCode, error);
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
