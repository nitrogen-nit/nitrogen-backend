package vn.nitrogen.platform.observability;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class CorrelationId {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private static final int MAX_LENGTH = 128;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]*");

    private CorrelationId() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static Optional<String> normalize(String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }

        String value = candidate.trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || !ALLOWED.matcher(value).matches()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    public static String from(String candidate) {
        return normalize(candidate).orElseGet(CorrelationId::newId);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }

    public static String currentOrNew() {
        return current().orElseGet(CorrelationId::newId);
    }

    public static UUID currentUuidOrNew() {
        return current()
                .flatMap(CorrelationId::toUuid)
                .orElseGet(UUID::randomUUID);
    }

    private static Optional<UUID> toUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
