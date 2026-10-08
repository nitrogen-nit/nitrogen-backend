package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Token dùng một lần để xác minh email sau khi đăng ký.
 *
 * <p>Phần chung với các token khác nằm ở {@link AbstractUserToken}; ở đây chỉ
 * còn dấu đã sử dụng.
 */
@Entity
@Table(schema = "identity", name = "email_verification_tokens")
public class EmailVerificationToken extends AbstractUserToken {

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected EmailVerificationToken() {
    }

    private EmailVerificationToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        super(user, tokenHash, issuedAt, expiresAt);
    }

    public static EmailVerificationToken issue(
            User user,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new EmailVerificationToken(user, tokenHash, issuedAt, expiresAt);
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
