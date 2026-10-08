package vn.nitrogen.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
@Tag("integration")
class IdempotentConsumerIntegrationTest extends TestcontainersBase {

    private static final String CONSUMER_NAME = "identity-user-projection";
    private static final String EFFECT_KEY = "user-created-count";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private IdempotentConsumer consumer;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS integration.consumer_test_effects (
                    effect_key VARCHAR(120) PRIMARY KEY,
                    executions INTEGER NOT NULL
                )
                """);
        jdbc.update("DELETE FROM integration.processed_messages");
        jdbc.update("DELETE FROM integration.consumer_test_effects");
        jdbc.update(
                "INSERT INTO integration.consumer_test_effects (effect_key, executions) VALUES (?, 0)",
                EFFECT_KEY);
    }

    @Test
    void duplicateDeliveryDoesNotRepeatBusinessSideEffect() throws Exception {
        MessageEnvelope envelope = envelope();

        MessageProcessingResult first = consumer.process(CONSUMER_NAME, envelope, this::incrementEffect);
        MessageProcessingResult duplicate = consumer.process(CONSUMER_NAME, envelope, this::incrementEffect);

        assertThat(first).isEqualTo(MessageProcessingResult.PROCESSED);
        assertThat(duplicate).isEqualTo(MessageProcessingResult.DUPLICATE);
        assertThat(executionCount()).isEqualTo(1);
        assertThat(processedMessageCount(envelope.messageId())).isEqualTo(1);
    }

    @Test
    void handlerFailureRollsBackDedupMarkerAndBusinessSideEffect() throws Exception {
        MessageEnvelope envelope = envelope();

        assertThatThrownBy(() -> consumer.process(CONSUMER_NAME, envelope, message -> {
                    incrementEffect(message);
                    throw new IllegalStateException("business failure");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("business failure");

        assertThat(executionCount()).isZero();
        assertThat(processedMessageCount(envelope.messageId())).isZero();

        assertThat(consumer.process(CONSUMER_NAME, envelope, this::incrementEffect))
                .isEqualTo(MessageProcessingResult.PROCESSED);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void concurrentDeliveryHasOneWinner() throws Exception {
        MessageEnvelope envelope = envelope();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MessageProcessingResult> first = executor.submit(() -> {
                start.await();
                return consumer.process(CONSUMER_NAME, envelope, this::incrementEffect);
            });
            Future<MessageProcessingResult> second = executor.submit(() -> {
                start.await();
                return consumer.process(CONSUMER_NAME, envelope, this::incrementEffect);
            });

            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(
                            MessageProcessingResult.PROCESSED,
                            MessageProcessingResult.DUPLICATE);
        }

        assertThat(executionCount()).isEqualTo(1);
        assertThat(processedMessageCount(envelope.messageId())).isEqualTo(1);
    }

    private void incrementEffect(MessageEnvelope ignored) {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        jdbc.update("""
                UPDATE integration.consumer_test_effects
                SET executions = executions + 1
                WHERE effect_key = ?
                """, EFFECT_KEY);
    }

    private int executionCount() {
        return jdbc.queryForObject(
                "SELECT executions FROM integration.consumer_test_effects WHERE effect_key = ?",
                Integer.class,
                EFFECT_KEY);
    }

    private int processedMessageCount(UUID messageId) {
        return jdbc.queryForObject("""
                SELECT count(*)
                FROM integration.processed_messages
                WHERE consumer_name = ? AND message_id = ?
                """, Integer.class, CONSUMER_NAME, messageId);
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
                MAPPER.readTree("{\"email\":\"user@example.com\"}"));
    }
}
