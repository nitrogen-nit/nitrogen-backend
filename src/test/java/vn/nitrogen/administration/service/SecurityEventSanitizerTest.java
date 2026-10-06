package vn.nitrogen.administration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SecurityEventSanitizerTest {

    private final SecurityEventSanitizer sanitizer =
            new SecurityEventSanitizer(new AuditRedactor());

    @Test
    void sanitizeIpAddressReturnsNullForBlankValues() {
        assertThat(sanitizer.sanitizeIpAddress(null)).isNull();
        assertThat(sanitizer.sanitizeIpAddress("   ")).isNull();
    }

    @Test
    void sanitizeIpAddressTrimsAndLimitsLength() {
        String tooLong = "12345678901234567890123456789012345678901234567890";

        assertThat(sanitizer.sanitizeIpAddress(" 127.0.0.1 "))
                .isEqualTo("127.0.0.1");
        assertThat(sanitizer.sanitizeIpAddress(tooLong))
                .hasSize(45);
    }

    @Test
    void sanitizeUserAgentRemovesControlCharactersAndLimitsLength() {
        String userAgent = "Mozilla\nInjected\rHeader";

        assertThat(sanitizer.sanitizeUserAgent(userAgent))
                .isEqualTo("MozillaInjectedHeader");
    }

    @Test
    void sanitizeReasonRedactsSecrets() {
        assertThat(sanitizer.sanitizeReason("password=plain token=abc reason=bad-login"))
                .contains("password=[REDACTED]", "token=[REDACTED]", "reason=bad-login")
                .doesNotContain("plain", "abc");
    }
}
