package vn.nitrogen.integration.api;

import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.integration.service.ProcessedMessageService;

/** Transaction boundary dùng chung cho consumer idempotent. */
@Profile("core")
@Controller
@Lazy
public class ProcessedMessageApi implements ModuleApi {

    private final ProcessedMessageService processedMessages;

    public ProcessedMessageApi(ProcessedMessageService processedMessages) {
        this.processedMessages = processedMessages;
    }

    public boolean process(
            String consumerName,
            UUID messageId,
            UUID correlationId,
            ProcessedMessageAction action) throws Exception {
        return processedMessages.process(consumerName, messageId, correlationId, action);
    }
}
