package vn.nitrogen.platform.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Đẩy {@code integration.outbox_events} sang RabbitMQ.
 *
 * <p>Quy tắc vận hành theo §11.4: claim một batch bằng {@code SKIP LOCKED},
 * ghi {@code locked_by}/{@code locked_until} rồi COMMIT; gọi RabbitMQ NGOÀI
 * transaction; sau đó mới đánh dấu PUBLISHED hoặc cập nhật lịch retry.
 *
 * <p>Chỉ chạy ở profile {@code web} — profile này sở hữu scheduler. Nếu bật ở
 * mọi replica sẽ có nhiều publisher tranh nhau cùng một batch.
 *
 * <p>TODO: hiện thực claim/publish/mark. Hiện chỉ là khung để lịch chạy có chỗ đứng.
 */
@Component
@Profile("web")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    @Scheduled(fixedDelayString = "${nitrogen.outbox.publish-interval:PT5S}")
    public void publishPendingBatch() {
        log.trace("Outbox publisher chưa được hiện thực");
    }
}
