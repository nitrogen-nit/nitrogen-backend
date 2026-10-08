package vn.nitrogen.platform.messaging;

@FunctionalInterface
public interface IdempotentMessageHandler {

    void handle(MessageEnvelope envelope) throws Exception;
}
