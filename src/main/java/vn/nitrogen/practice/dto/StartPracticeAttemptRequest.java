package vn.nitrogen.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record StartPracticeAttemptRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String attemptKind,
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String originType,
        @NotNull UUID originId,
        @Size(max = 100) String idempotencyKey,
        Instant deadlineAt) {
}
