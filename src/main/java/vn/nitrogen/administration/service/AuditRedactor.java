package vn.nitrogen.administration.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuditRedactor {

    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)(password|token|secret|authorization|refresh_token|access_token|reset_token|verification_token)\\s*=\\s*[^\\s,;]+");

    public String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return SENSITIVE_ASSIGNMENT.matcher(value)
                .replaceAll(match -> match.group(1) + "=[REDACTED]");
    }
}
