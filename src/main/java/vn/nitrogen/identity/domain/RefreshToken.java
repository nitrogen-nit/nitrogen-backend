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
@Table(schema = "identity", name = "refresh_tokens")
public class RefreshToken extends AbstractIdentifiableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected RefreshToken() {
    }
    private RefreshToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(long rowVersion) {
        this.rowVersion = rowVersion;
    }
    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
    public static RefreshToken issue(
            User user,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new RefreshToken(user, tokenHash, issuedAt, expiresAt);
    }
}
