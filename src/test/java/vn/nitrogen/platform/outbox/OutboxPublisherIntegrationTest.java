package vn.nitrogen.platform.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import vn.nitrogen.integration.api.OutboxApi;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.domain.OutboxStatus;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.repository.OutboxEventRepository;
import vn.nitrogen.platform.messaging.MessageEnvelope;
import vn.nitrogen.platform.messaging.MessagingTopology;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
@Tag("integration")
class OutboxPublisherIntegrationTest extends TestcontainersBase {

    private static final String ROUTING_KEY = "test.outbox.published";

    @Autowired
    private OutboxApi outbox;

    @Autowired
    private OutboxEventRepository outboxEvents;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String queueName;

    @BeforeEach
    void setUp() {
        outboxEvents.deleteAllInBatch();
        queueName = "nitrogen.test.outbox." + UUID.randomUUID();
    }

    @AfterEach
    void cleanUp() {
        rabbitAdmin.deleteQueue(queueName);
    }

    @Test
    void publishesEnvelopeAfterClaimAndMarksEventPublished() {
        declareBoundQueue();
        AppendOutboxCommand command = command(ROUTING_KEY);
        UUID eventId = append(command);

        publisher(3).publishPendingBatch();

        Object received = rabbit.receiveAndConvert(queueName, 5_000);
        assertThat(received).isInstanceOf(MessageEnvelope.class);
        MessageEnvelope envelope = (MessageEnvelope) received;
        assertThat(envelope.messageId()).isEqualTo(eventId);
        assertThat(envelope.messageType()).isEqualTo(command.eventType());
        assertThat(envelope.correlationId()).isEqualTo(command.correlationId());
        assertThat(envelope.payload().get("email").asText()).isEqualTo("user@example.com");

        OutboxEvent stored = outboxEvents.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(stored.getPublishedAt()).isNotNull();
        assertThat(stored.getLockedBy()).isNull();
    }

    @Test
    void reschedulesReturnedMessageAndPersistsFailureReason() {
        UUID eventId = append(command("test.outbox.unroutable." + UUID.randomUUID()));

        publisher(3).publishPendingBatch();

        OutboxEvent stored = outboxEvents.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stored.getRetryCount()).isEqualTo(1);
        assertThat(stored.getNextRetryAt()).isAfter(stored.getOccurredAt());
        assertThat(stored.getLastErrorCode()).isEqualTo("RABBIT_MESSAGE_RETURNED");
        assertThat(stored.getLastError()).isNotBlank();
        assertThat(stored.getLockedBy()).isNull();
    }

    @Test
    void marksReturnedMessageFailedWhenRetryBudgetIsExhausted() {
        UUID eventId = append(command("test.outbox.failed." + UUID.randomUUID()));

        publisher(1).publishPendingBatch();

        OutboxEvent stored = outboxEvents.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(stored.getRetryCount()).isEqualTo(1);
        assertThat(stored.getFailedAt()).isNotNull();
        assertThat(stored.getLastErrorCode()).isEqualTo("RABBIT_MESSAGE_RETURNED");
        assertThat(stored.getLockedBy()).isNull();
    }

    private void declareBoundQueue() {
        Queue queue = new Queue(queueName, false, false, true);
        Binding binding = BindingBuilder.bind(queue)
                .to(new TopicExchange(MessagingTopology.EVENT_EXCHANGE))
                .with(ROUTING_KEY);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
    }

    private OutboxPublisher publisher(int maxRetries) {
        return new OutboxPublisher(
                outbox,
                rabbit,
                objectMapper,
                new OutboxPublisherSettings(
                        "publisher-test",
                        10,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(5),
                        new OutboxRetryPolicy(
                                maxRetries,
                                Duration.ofSeconds(1),
                                Duration.ofMinutes(1))));
    }

    private UUID append(AppendOutboxCommand command) {
        return new TransactionTemplate(transactionManager)
                .execute(status -> outbox.append(command));
    }

    private static AppendOutboxCommand command(String routingKey) {
        return new AppendOutboxCommand(
                "USER",
                UUID.randomUUID(),
                "UserRegistered",
                routingKey,
                1,
                UUID.randomUUID(),
                null,
                Map.of("email", "user@example.com"));
    }
}
