package vn.nitrogen.platform.outbox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vn.nitrogen.integration.api.OutboxApi;
import vn.nitrogen.integration.dto.ClaimedOutboxEvent;
import vn.nitrogen.platform.messaging.MessageEnvelope;
import vn.nitrogen.platform.messaging.MessagingTopology;

/**
 * Đẩy {@code integration.outbox_events} sang RabbitMQ.
 *
 * <p>Quy tắc vận hành theo §11.4: claim một batch bằng {@code SKIP LOCKED},
 * ghi {@code locked_by}/{@code locked_until} rồi COMMIT; gọi RabbitMQ NGOÀI
 * transaction; sau đó mới đánh dấu PUBLISHED hoặc cập nhật lịch retry.
 *
 * <p>Chỉ chạy ở profile {@code web}. Mỗi replica có worker id riêng; lease và
 * {@code SKIP LOCKED} cho phép nhiều replica chia tải mà không cùng claim một row.
 */
@Component
@Profile("web")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String CONFIRM_NACK = "RABBIT_CONFIRM_NACK";
    private static final String MESSAGE_RETURNED = "RABBIT_MESSAGE_RETURNED";
    private static final String PUBLISH_TIMEOUT = "RABBIT_CONFIRM_TIMEOUT";
    private static final String PUBLISH_ERROR = "RABBIT_PUBLISH_ERROR";
    private static final String INTERRUPTED = "RABBIT_PUBLISH_INTERRUPTED";

    private final OutboxApi outbox;
    private final RabbitTemplate rabbit;
    private final ObjectMapper objectMapper;
    private final OutboxPublisherSettings settings;

    public OutboxPublisher(
            OutboxApi outbox,
            RabbitTemplate rabbit,
            ObjectMapper objectMapper,
            OutboxPublisherSettings settings) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.objectMapper = objectMapper;
        this.settings = settings;
    }

    @Scheduled(fixedDelayString = "${nitrogen.outbox.publish-interval:PT5S}")
    public void publishPendingBatch() {
        Instant now = Instant.now();
        List<ClaimedOutboxEvent> events = outbox.claimPending(
                settings.workerId(),
                now,
                settings.leaseDuration(),
                settings.batchSize());

        events.forEach(this::publish);
    }

    private void publish(ClaimedOutboxEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.id().toString());
            rabbit.convertAndSend(
                    MessagingTopology.EVENT_EXCHANGE,
                    event.routingKey(),
                    toEnvelope(event),
                    correlation);

            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(settings.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            ReturnedMessage returned = correlation.getReturned();

            if (!confirm.ack()) {
                recordFailure(event, CONFIRM_NACK, safeMessage(confirm.reason()));
            } else if (returned != null) {
                recordFailure(event, MESSAGE_RETURNED, returned.getReplyText());
            } else {
                outbox.markPublished(event.id(), settings.workerId(), Instant.now());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordFailure(event, INTERRUPTED, safeMessage(exception.getMessage()));
        } catch (TimeoutException exception) {
            recordFailure(event, PUBLISH_TIMEOUT, safeMessage(exception.getMessage()));
        } catch (ExecutionException exception) {
            recordFailure(event, PUBLISH_ERROR, safeMessage(exception.getCause()));
        } catch (RuntimeException exception) {
            recordFailure(event, PUBLISH_ERROR, safeMessage(exception));
        }
    }

    private MessageEnvelope toEnvelope(ClaimedOutboxEvent event) {
        return new MessageEnvelope(
                event.id(),
                event.eventType(),
                event.schemaVersion(),
                event.correlationId(),
                event.causationId(),
                event.aggregateType(),
                event.aggregateId(),
                event.occurredAt(),
                objectMapper.valueToTree(event.payload()));
    }

    private void recordFailure(ClaimedOutboxEvent event, String errorCode, String error) {
        int retryCount = event.retryCount() + 1;
        Instant now = Instant.now();
        try {
            if (settings.retryPolicy().exhausted(retryCount)) {
                outbox.markFailed(
                        event.id(), settings.workerId(), retryCount, now, errorCode, error);
                log.error("Outbox event {} exhausted {} retries: {}", event.id(), retryCount, error);
            } else {
                Instant nextRetryAt = now.plus(settings.retryPolicy().delayFor(retryCount));
                outbox.reschedule(
                        event.id(), settings.workerId(), retryCount, nextRetryAt, errorCode, error);
                log.warn("Outbox event {} rescheduled for {}: {}", event.id(), nextRetryAt, error);
            }
        } catch (RuntimeException stateUpdateFailure) {
            log.error("Could not update failed outbox event {}", event.id(), stateUpdateFailure);
        }
    }

    private static String safeMessage(Throwable throwable) {
        return throwable == null ? "Unknown RabbitMQ publishing error" : safeMessage(throwable.getMessage());
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "Unknown RabbitMQ publishing error" : message;
    }
}
