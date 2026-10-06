package vn.nitrogen.platform.messaging;

public final class MessagingTopology {
    public static final String EVENT_EXCHANGE = "nitrogen.events.v1";
    public static final String RETRY_EXCHANGE = "nitrogen.retry.v1";
    public static final String DEAD_LETTER_EXCHANGE = "nitrogen.dead-letter.v1";

    public static final String RETRY_30S_QUEUE = "nitrogen.retry.30s.v1";
    public static final String DEAD_LETTER_QUEUE = "nitrogen.events.dlq.v1";

    public static final String ALL_ROUTING_KEYS = "#";

    private MessagingTopology() {
    }
}
