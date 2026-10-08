package vn.nitrogen.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token thô lấy từ link trong email xác minh. {@code toString} không in token. */
public record VerifyEmailRequest(@NotBlank @Size(max = 128) String token) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[token=***]";
    }
}
