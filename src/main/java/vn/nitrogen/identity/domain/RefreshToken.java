package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Refresh token của một phiên đăng nhập.
 *
 * <p>Phần chung với password reset token nằm ở {@link AbstractUserToken};
 * ở đây chỉ còn dấu thu hồi.
 */
@Entity
@Table(schema = "identity", name = "refresh_tokens")
public class RefreshToken extends AbstractUserToken {

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
    }

    private RefreshToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        super(user, tokenHash, issuedAt, expiresAt);
    }

    public static RefreshToken issue(
            User user,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new RefreshToken(user, tokenHash, issuedAt, expiresAt);
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
