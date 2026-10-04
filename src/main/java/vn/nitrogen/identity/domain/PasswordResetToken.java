package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

@Entity
@Table(schema = "identity", name = "password_reset_tokens")
public class PasswordResetToken extends AbstractIdentifiableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected PasswordResetToken() {
    }

    private PasswordResetToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken issue(
            User user,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new PasswordResetToken(user, tokenHash, issuedAt, expiresAt);
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }
}