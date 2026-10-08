package vn.nitrogen.platform.messaging;

import com.rabbitmq.client.Channel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Consumer wrapper cho manual acknowledgement. ACK chỉ được gửi sau khi
 * transaction chứa dedup marker và business side effect đã commit.
 */
@Component
@Profile("core")
public class RabbitIdempotentConsumer {

    private final IdempotentConsumer consumer;

    public RabbitIdempotentConsumer(IdempotentConsumer consumer) {
        this.consumer = consumer;
    }

    public MessageProcessingResult processAndAcknowledge(
            String consumerName,
            MessageEnvelope envelope,
            Channel channel,
            long deliveryTag,
            IdempotentMessageHandler handler) throws Exception {
        MessageProcessingResult result = consumer.process(consumerName, envelope, handler);
        channel.basicAck(deliveryTag, false);
        return result;
    }
}
