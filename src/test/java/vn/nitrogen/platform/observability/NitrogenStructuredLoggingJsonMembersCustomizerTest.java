package vn.nitrogen.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;

@Tag("unit")
class NitrogenStructuredLoggingJsonMembersCustomizerTest {

    private final NitrogenStructuredLoggingJsonMembersCustomizer customizer =
            new NitrogenStructuredLoggingJsonMembersCustomizer();

    @Test
    void addsTimestampCorrelationTraceAndSpanFieldsFromMdc() {
        LoggingEvent event = eventWithMdc(Map.of(
                "correlationId", "request-123",
                "traceId", "abc123",
                "spanId", "def456"));

        String json = JsonWriter.<ILoggingEvent>of(customizer::customize).writeToString(event);

        assertThat(json).contains("\"timestamp\":\"2026-08-30T00:00:00Z\"");
        assertThat(json).contains("\"correlationId\":\"request-123\"");
        assertThat(json).contains("\"traceId\":\"abc123\"");
        assertThat(json).contains("\"spanId\":\"def456\"");
    }

    @Test
    void fallsBackToSnakeCaseTraceKeysAndEmptyValues() {
        LoggingEvent event = eventWithMdc(Map.of("trace_id", "abc123", "span_id", "def456"));
        LoggingEvent emptyEvent = eventWithMdc(Map.of());

        String json = JsonWriter.<ILoggingEvent>of(customizer::customize).writeToString(event);
        String emptyJson = JsonWriter.<ILoggingEvent>of(customizer::customize).writeToString(emptyEvent);

        assertThat(json).contains("\"traceId\":\"abc123\"");
        assertThat(json).contains("\"spanId\":\"def456\"");
        assertThat(emptyJson).contains("\"correlationId\":\"\"");
        assertThat(emptyJson).contains("\"traceId\":\"\"");
        assertThat(emptyJson).contains("\"spanId\":\"\"");
    }

    private static LoggingEvent eventWithMdc(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setLoggerName("vn.nitrogen.Test");
        event.setMessage("test");
        event.setInstant(Instant.parse("2026-08-30T00:00:00Z"));
        event.setMDCPropertyMap(mdc);
        return event;
    }
}
