package vn.nitrogen.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đăng ký tài khoản bằng email/password.
 *
 * <p>{@code toString} che password: record mặc định in mọi field, và một dòng
 * log vô tình in request là đủ để lộ mật khẩu (§18).
 */
public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName) {

    @Override
    public String toString() {
        return "RegisterUserRequest[email=%s, password=***, displayName=%s]".formatted(email, displayName);
    }
}
