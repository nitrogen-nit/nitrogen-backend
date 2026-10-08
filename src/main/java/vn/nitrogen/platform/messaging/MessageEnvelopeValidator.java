package vn.nitrogen.platform.messaging;

import org.springframework.stereotype.Component;

@Component
public class MessageEnvelopeValidator {

    private static final int MAX_MESSAGE_TYPE_LENGTH = 120;
    private static final int MAX_AGGREGATE_TYPE_LENGTH = 80;

    public void validate(MessageEnvelope envelope) {
        if (envelope == null) {
            throw invalid("envelope must not be null");
        }

        requirePresent(envelope.messageId(), "messageId");
        requireNonBlank(envelope.messageType(), "messageType", MAX_MESSAGE_TYPE_LENGTH);
        if (envelope.schemaVersion() <= 0) {
            throw invalid("schemaVersion must be positive");
        }
        requirePresent(envelope.correlationId(), "correlationId");
        requireNonBlank(envelope.aggregateType(), "aggregateType", MAX_AGGREGATE_TYPE_LENGTH);
        requirePresent(envelope.aggregateId(), "aggregateId");
        requirePresent(envelope.occurredAt(), "occurredAt");
        requirePresent(envelope.payload(), "payload");
        if (!envelope.payload().isObject()) {
            throw invalid("payload must be a JSON object");
        }
    }

    private static void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw invalid(fieldName + " must not be null");
        }
    }

    private static void requireNonBlank(String value, String fieldName, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + " must not be blank");
        }
        if (value.trim().length() > maximumLength) {
            throw invalid(fieldName + " must not exceed " + maximumLength + " characters");
        }
    }

    private static InvalidMessageEnvelopeException invalid(String message) {
        return new InvalidMessageEnvelopeException(message);
    }
}
