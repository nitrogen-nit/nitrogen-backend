package vn.nitrogen.platform.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("unit")
class MessageEnvelopeValidatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final MessageEnvelopeValidator validator = new MessageEnvelopeValidator();

    @Test
    void acceptsValidEnvelope() throws Exception {
        assertThatCode(() -> validator.validate(validEnvelope()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingEnvelopeAndIdentifiers() throws Exception {
        MessageEnvelope valid = validEnvelope();
        MessageEnvelope missingMessageId = copy(valid, null, valid.messageType(), valid.schemaVersion(), valid.payload());
        MessageEnvelope missingCorrelationId = new MessageEnvelope(
                valid.messageId(),
                valid.messageType(),
                valid.schemaVersion(),
                null,
                valid.causationId(),
                valid.aggregateType(),
                valid.aggregateId(),
                valid.occurredAt(),
                valid.payload());

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("envelope");
        assertThatThrownBy(() -> validator.validate(missingMessageId))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("messageId");
        assertThatThrownBy(() -> validator.validate(missingCorrelationId))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("correlationId");
    }

    @Test
    void rejectsInvalidContractMetadata() throws Exception {
        MessageEnvelope valid = validEnvelope();
        MessageEnvelope blankMessageType = copy(valid, valid.messageId(), " ", valid.schemaVersion(), valid.payload());
        MessageEnvelope invalidVersion = copy(valid, valid.messageId(), valid.messageType(), 0, valid.payload());
        MessageEnvelope blankAggregateType = new MessageEnvelope(
                valid.messageId(),
                valid.messageType(),
                valid.schemaVersion(),
                valid.correlationId(),
                valid.causationId(),
                " ",
                valid.aggregateId(),
                valid.occurredAt(),
                valid.payload());

        assertThatThrownBy(() -> validator.validate(blankMessageType))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("messageType");
        assertThatThrownBy(() -> validator.validate(invalidVersion))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("schemaVersion");
        assertThatThrownBy(() -> validator.validate(blankAggregateType))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("aggregateType");
    }

    @Test
    void rejectsNonObjectPayload() throws Exception {
        MessageEnvelope valid = validEnvelope();
        JsonNode arrayPayload = MAPPER.readTree("[]");
        MessageEnvelope invalid = copy(
                valid,
                valid.messageId(),
                valid.messageType(),
                valid.schemaVersion(),
                arrayPayload);

        assertThatThrownBy(() -> validator.validate(invalid))
                .isInstanceOf(InvalidMessageEnvelopeException.class)
                .hasMessageContaining("JSON object");
    }

    private static MessageEnvelope copy(
            MessageEnvelope source,
            UUID messageId,
            String messageType,
            int schemaVersion,
            JsonNode payload) {
        return new MessageEnvelope(
                messageId,
                messageType,
                schemaVersion,
                source.correlationId(),
                source.causationId(),
                source.aggregateType(),
                source.aggregateId(),
                source.occurredAt(),
                payload);
    }

    private static MessageEnvelope validEnvelope() throws Exception {
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
