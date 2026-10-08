package vn.nitrogen.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rabbitmq.client.Channel;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@Tag("unit")
class RabbitIdempotentConsumerTest {

    @Test
    void acknowledgesProcessedMessageAfterTransactionWrapperReturns() throws Exception {
        StubIdempotentConsumer consumer =
                StubIdempotentConsumer.returning(MessageProcessingResult.PROCESSED);
        RecordingChannel channel = new RecordingChannel();
        RabbitIdempotentConsumer rabbitConsumer = new RabbitIdempotentConsumer(consumer);

        MessageProcessingResult result = rabbitConsumer.processAndAcknowledge(
                "consumer", envelope(), channel.channel(), 42L, message -> {});

        assertThat(result).isEqualTo(MessageProcessingResult.PROCESSED);
        assertThat(channel.acknowledgedDeliveryTag()).isEqualTo(42L);
        assertThat(channel.multiple()).isFalse();
    }

    @Test
    void acknowledgesDuplicateMessage() throws Exception {
        StubIdempotentConsumer consumer =
                StubIdempotentConsumer.returning(MessageProcessingResult.DUPLICATE);
        RecordingChannel channel = new RecordingChannel();
        RabbitIdempotentConsumer rabbitConsumer = new RabbitIdempotentConsumer(consumer);

        MessageProcessingResult result = rabbitConsumer.processAndAcknowledge(
                "consumer", envelope(), channel.channel(), 43L, message -> {});

        assertThat(result).isEqualTo(MessageProcessingResult.DUPLICATE);
        assertThat(channel.acknowledgedDeliveryTag()).isEqualTo(43L);
    }

    @Test
    void doesNotAcknowledgeWhenHandlerFails() throws Exception {
        StubIdempotentConsumer consumer =
                StubIdempotentConsumer.throwing(new IllegalStateException("business failure"));
        RecordingChannel channel = new RecordingChannel();
        RabbitIdempotentConsumer rabbitConsumer = new RabbitIdempotentConsumer(consumer);
        MessageEnvelope envelope = envelope();

        assertThatThrownBy(() -> rabbitConsumer.processAndAcknowledge(
                    "consumer", envelope, channel.channel(), 44L, message -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");
        assertThat(channel.acknowledgedDeliveryTag()).isNull();
    }

    private static MessageEnvelope envelope() throws Exception {
        return new MessageEnvelope(
                UUID.randomUUID(),
                "UserRegistered",
                1,
                UUID.randomUUID(),
                null,
                "USER",
                UUID.randomUUID(),
                Instant.parse("2026-10-08T00:00:00Z"),
                JsonMapper.builder().build().readTree("{}"));
    }

    private static final class StubIdempotentConsumer extends IdempotentConsumer {

        private final MessageProcessingResult result;
        private final Exception failure;

        private StubIdempotentConsumer(MessageProcessingResult result, Exception failure) {
            super(null, null);
            this.result = result;
            this.failure = failure;
        }

        private static StubIdempotentConsumer returning(MessageProcessingResult result) {
            return new StubIdempotentConsumer(result, null);
        }

        private static StubIdempotentConsumer throwing(Exception failure) {
            return new StubIdempotentConsumer(null, failure);
        }

        @Override
        public MessageProcessingResult process(
                String consumerName,
                MessageEnvelope envelope,
                IdempotentMessageHandler handler) throws Exception {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private static final class RecordingChannel {

        private Long acknowledgedDeliveryTag;
        private boolean multiple;

        private Channel channel() {
            return (Channel) Proxy.newProxyInstance(
                    Channel.class.getClassLoader(),
                    new Class<?>[] {Channel.class},
                    (proxy, method, arguments) -> {
                        if ("basicAck".equals(method.getName())) {
                            acknowledgedDeliveryTag = (Long) arguments[0];
                            multiple = (Boolean) arguments[1];
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private Long acknowledgedDeliveryTag() {
            return acknowledgedDeliveryTag;
        }

        private boolean multiple() {
            return multiple;
        }

        private static Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive() || returnType == void.class) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return 0;
        }
    }
}
