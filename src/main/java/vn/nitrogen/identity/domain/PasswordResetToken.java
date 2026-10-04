package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Token dùng một lần cho luồng đặt lại mật khẩu.
 *
 * <p>Phần chung với refresh token nằm ở {@link AbstractUserToken}; ở đây chỉ
 * còn dấu đã sử dụng.
 */
@Entity
@Table(schema = "identity", name = "password_reset_tokens")
public class PasswordResetToken extends AbstractUserToken {

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected PasswordResetToken() {
    }

    private PasswordResetToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        super(user, tokenHash, issuedAt, expiresAt);
    }

    public static PasswordResetToken issue(
            User user,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new PasswordResetToken(user, tokenHash, issuedAt, expiresAt);
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }
}
