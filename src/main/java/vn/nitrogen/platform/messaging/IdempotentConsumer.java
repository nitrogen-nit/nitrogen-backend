package vn.nitrogen.platform.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import vn.nitrogen.integration.api.ProcessedMessageApi;

/** Validate envelope và chạy handler đúng một lần theo consumer/message id. */
@Component
@Profile("core")
public class IdempotentConsumer {

    private final MessageEnvelopeValidator validator;
    private final ProcessedMessageApi processedMessages;

    public IdempotentConsumer(
            MessageEnvelopeValidator validator,
            ProcessedMessageApi processedMessages) {
        this.validator = validator;
        this.processedMessages = processedMessages;
    }

    public MessageProcessingResult process(
            String consumerName,
            MessageEnvelope envelope,
            IdempotentMessageHandler handler) throws Exception {
        validator.validate(envelope);

        boolean processed = processedMessages.process(
                consumerName,
                envelope.messageId(),
                envelope.correlationId(),
                () -> handler.handle(envelope));

        return processed
                ? MessageProcessingResult.PROCESSED
                : MessageProcessingResult.DUPLICATE;
    }
}
