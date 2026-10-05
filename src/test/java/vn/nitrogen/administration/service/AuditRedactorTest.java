package vn.nitrogen.administration.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AuditRedactorTest {

    private final AuditRedactor redactor = new AuditRedactor();

    @Test
    void keepsNullAndBlankValuesUnchanged() {
        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("   ")).isEqualTo("   ");
    }

    @Test
    void redactsSensitiveAssignmentsCaseInsensitively() {
        String result = redactor.redact(
                "Password=plain token=abc SECRET=hidden authorization=BearerValue note=public");

        assertThat(result)
                .contains(
                        "Password=[REDACTED]",
                        "token=[REDACTED]",
                        "SECRET=[REDACTED]",
                        "authorization=[REDACTED]",
                        "note=public")
                .doesNotContain("plain", "abc", "hidden", "BearerValue");
    }
}
