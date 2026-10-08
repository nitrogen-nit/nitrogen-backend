package vn.nitrogen.integration.service;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.integration.api.ProcessedMessageAction;
import vn.nitrogen.integration.repository.ProcessedMessageRepository;

@Service
@Transactional(readOnly = true)
public class ProcessedMessageService {

    private final ProcessedMessageRepository processedMessages;

    public ProcessedMessageService(ProcessedMessageRepository processedMessages) {
        this.processedMessages = processedMessages;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean process(
            String consumerName,
            UUID messageId,
            UUID correlationId,
            ProcessedMessageAction action) throws Exception {
        String normalizedConsumerName = requireConsumerName(consumerName);
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(action, "action");

        if (!processedMessages.tryInsert(normalizedConsumerName, messageId, correlationId)) {
            return false;
        }

        action.run();
        return true;
    }

    private static String requireConsumerName(String consumerName) {
        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("consumerName must not be blank");
        }
        String normalized = consumerName.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("consumerName must not exceed 120 characters");
        }
        return normalized;
    }
}
