package vn.nitrogen.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.rabbitmq.client.GetResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@Tag("docker")
@Tag("integration")
class RabbitTopologyIntegrationTest extends TestcontainersBase {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Declarables nitrogenTopology;

    @BeforeEach
    void purgeSharedQueues() {
        rabbitAdmin.purgeQueue(MessagingTopology.RETRY_30S_QUEUE, false);
        rabbitAdmin.purgeQueue(MessagingTopology.DEAD_LETTER_QUEUE, false);
    }

    @AfterEach
    void purgeDeadLetterQueue() {
        rabbitAdmin.purgeQueue(MessagingTopology.DEAD_LETTER_QUEUE, false);
    }

    @Test
    void declaresDurableExchangesAndQueuesWithExpectedArguments() {
        assertExchangeExists(MessagingTopology.EVENT_EXCHANGE);
        assertExchangeExists(MessagingTopology.RETRY_EXCHANGE);
        assertExchangeExists(MessagingTopology.DEAD_LETTER_EXCHANGE);

        assertThat(rabbitAdmin.getQueueProperties(MessagingTopology.RETRY_30S_QUEUE)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(MessagingTopology.DEAD_LETTER_QUEUE)).isNotNull();

        Queue retryQueue = topologyQueue(MessagingTopology.RETRY_30S_QUEUE);
        assertThat(retryQueue.isDurable()).isTrue();
        assertThat(retryQueue.getArguments())
                .containsEntry("x-queue-type", "quorum")
                .containsEntry("x-message-ttl", 30_000)
                .containsEntry("x-dead-letter-exchange", MessagingTopology.EVENT_EXCHANGE);

        Queue deadLetterQueue = topologyQueue(MessagingTopology.DEAD_LETTER_QUEUE);
        assertThat(deadLetterQueue.isDurable()).isTrue();
        assertThat(deadLetterQueue.getArguments()).containsEntry("x-queue-type", "quorum");
    }

    @Test
    void provisionsTopologyIdempotently() {
        rabbitAdmin.initialize();
        rabbitAdmin.initialize();

        assertThat(rabbitAdmin.getQueueProperties(MessagingTopology.RETRY_30S_QUEUE)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(MessagingTopology.DEAD_LETTER_QUEUE)).isNotNull();
    }

    @Test
    void routesMessagesFromRetryExchangeToRetryQueue() {
        String payload = "retry-routing-" + UUID.randomUUID();

        rabbitTemplate.convertAndSend(
                MessagingTopology.RETRY_EXCHANGE, "identity.user.registered", payload);

        assertThat(rabbitTemplate.receiveAndConvert(MessagingTopology.RETRY_30S_QUEUE, 5_000))
                .isEqualTo(payload);
    }

    @Test
    void convertsMessageEnvelopeWithTimeAndJsonPayload() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        MessageEnvelope envelope = new MessageEnvelope(
                UUID.randomUUID(),
                "AttemptSubmitted",
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PRACTICE_ATTEMPT",
                aggregateId,
                Instant.parse("2026-10-07T00:00:00Z"),
                JsonMapper.builder()
                        .build()
                        .readTree("{\"attemptId\":\"" + aggregateId + "\"}"));

        rabbitTemplate.convertAndSend(
                MessagingTopology.RETRY_EXCHANGE, "practice.attempt.submitted", envelope);

        assertThat(rabbitTemplate.receiveAndConvert(MessagingTopology.RETRY_30S_QUEUE, 5_000))
                .isEqualTo(envelope);
    }

    @Test
    void routesRejectedMessagesToDeadLetterQueue() {
        String sourceQueueName = "nitrogen.test.rejected." + UUID.randomUUID();
        Queue sourceQueue = QueueBuilder.nonDurable(sourceQueueName)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey("test.rejected")
                .build();

        try {
            rabbitAdmin.declareQueue(sourceQueue);
            String payload = "rejected-" + UUID.randomUUID();
            rabbitTemplate.convertAndSend("", sourceQueueName, payload);

            rabbitTemplate.execute(channel -> {
                GetResponse response = channel.basicGet(sourceQueueName, false);
                assertThat(response).isNotNull();
                channel.basicReject(response.getEnvelope().getDeliveryTag(), false);
                return null;
            });

            assertThat(rabbitTemplate.receiveAndConvert(MessagingTopology.DEAD_LETTER_QUEUE, 5_000))
                    .isEqualTo(payload);
        } finally {
            rabbitAdmin.deleteQueue(sourceQueueName);
        }
    }

    @Test
    void returnsExpiredMessagesToEventExchange() {
        String suffix = UUID.randomUUID().toString();
        String retryQueueName = "nitrogen.test.retry." + suffix;
        String destinationQueueName = "nitrogen.test.destination." + suffix;

        Queue retryQueue = QueueBuilder.nonDurable(retryQueueName)
                .ttl(500)
                .deadLetterExchange(MessagingTopology.EVENT_EXCHANGE)
                .build();
        Queue destinationQueue = QueueBuilder.nonDurable(destinationQueueName).build();
        TopicExchange eventExchange = new TopicExchange(MessagingTopology.EVENT_EXCHANGE);
        Binding destinationBinding = BindingBuilder.bind(destinationQueue)
                .to(eventExchange)
                .with(retryQueueName);

        try {
            rabbitAdmin.declareQueue(retryQueue);
            rabbitAdmin.declareQueue(destinationQueue);
            rabbitAdmin.declareBinding(destinationBinding);

            String payload = "expired-" + UUID.randomUUID();
            rabbitTemplate.convertAndSend("", retryQueueName, payload);

            assertThat(rabbitTemplate.receiveAndConvert(destinationQueueName, 10_000))
                    .isEqualTo(payload);
        } finally {
            rabbitAdmin.deleteQueue(retryQueueName);
            rabbitAdmin.deleteQueue(destinationQueueName);
        }
    }

    private void assertExchangeExists(String exchangeName) {
        rabbitTemplate.execute(channel -> {
            assertThat(channel.exchangeDeclarePassive(exchangeName)).isNotNull();
            return null;
        });
    }

    private Queue topologyQueue(String queueName) {
        return nitrogenTopology.getDeclarablesByType(Queue.class).stream()
                .filter(queue -> queueName.equals(queue.getName()))
                .findFirst()
                .orElseThrow();
    }
}
