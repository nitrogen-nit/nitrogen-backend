package vn.nitrogen.platform.observability;

import java.util.Map;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public class NitrogenStructuredLoggingJsonMembersCustomizer
        implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(Members<ILoggingEvent> members) {
        members.add("timestamp", event -> event.getInstant().toString());
        members.add(CorrelationId.MDC_KEY, event -> mdcValue(event, CorrelationId.MDC_KEY));
        members.add("traceId", event -> mdcValue(event, "traceId", "trace_id"));
        members.add("spanId", event -> mdcValue(event, "spanId", "span_id"));
    }

    private static String mdcValue(ILoggingEvent event, String... keys) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        for (String key : keys) {
            String value = mdc.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
