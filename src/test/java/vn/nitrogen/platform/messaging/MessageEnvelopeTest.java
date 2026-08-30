package vn.nitrogen.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import vn.nitrogen.platform.observability.CorrelationId;

@Tag("unit")
class MessageEnvelopeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void exposesMessageContractFields() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-30T00:00:00Z");

        MessageEnvelope envelope = new MessageEnvelope(
                messageId,
                "AttemptSubmitted",
                1,
                correlationId,
                causationId,
                "PRACTICE_ATTEMPT",
                aggregateId,
                occurredAt,
                MAPPER.readTree("{\"attemptId\":\"" + aggregateId + "\"}"));

        assertThat(envelope.messageId()).isEqualTo(messageId);
        assertThat(envelope.messageType()).isEqualTo("AttemptSubmitted");
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.correlationId()).isEqualTo(correlationId);
        assertThat(envelope.causationId()).isEqualTo(causationId);
        assertThat(envelope.aggregateType()).isEqualTo("PRACTICE_ATTEMPT");
        assertThat(envelope.aggregateId()).isEqualTo(aggregateId);
        assertThat(envelope.occurredAt()).isEqualTo(occurredAt);
        assertThat(envelope.payload().get("attemptId").asText()).isEqualTo(aggregateId.toString());
    }

    @Test
    void canUseCurrentHttpCorrelationIdForMessageEnvelopeWhenItIsUuid() throws Exception {
        UUID correlationId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        MDC.put(CorrelationId.MDC_KEY, correlationId.toString());

        MessageEnvelope envelope = new MessageEnvelope(
                UUID.randomUUID(),
                "AttemptSubmitted",
                1,
                CorrelationId.currentUuidOrNew(),
                UUID.randomUUID(),
                "PRACTICE_ATTEMPT",
                aggregateId,
                Instant.parse("2026-08-30T00:00:00Z"),
                MAPPER.readTree("{\"attemptId\":\"" + aggregateId + "\"}"));

        assertThat(envelope.correlationId()).isEqualTo(correlationId);
    }
}
