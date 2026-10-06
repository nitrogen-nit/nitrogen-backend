package vn.nitrogen.administration.service;

import org.springframework.stereotype.Component;

@Component
public class SecurityEventSanitizer {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final AuditRedactor redactor;

    public SecurityEventSanitizer(AuditRedactor redactor) {
        this.redactor = redactor;
    }

    public String sanitizeIpAddress(String value) {
        return normalize(value, MAX_IP_LENGTH);
    }

    public String sanitizeUserAgent(String value) {
        String normalized = normalize(value, MAX_USER_AGENT_LENGTH);
        if (normalized == null) {
            return null;
        }

        return normalized.replaceAll("\\p{Cntrl}", "");
    }

    public String sanitizeReason(String value) {
        return redactor.redact(value);
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength);
    }
}
