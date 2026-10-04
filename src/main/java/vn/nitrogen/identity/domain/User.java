package vn.nitrogen.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

@Entity
@Table(schema = "identity", name = "users")
public class User extends AbstractIdentifiableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Set<String> getRoles() {
        return Set.copyOf(roles);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        roles.add(role);
    }

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            schema = "identity",
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 64)
    private Set<String> roles = new HashSet<>();

    protected User() {
    }

    private User(String email, String displayName, UserStatus status) {
        this.email = normalizeEmail(email);
        this.displayName = displayName;
        this.status = status;
    }

    public static User active(String email, String displayName) {
        return new User(email, displayName, UserStatus.ACTIVE);
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void disable() {
        status = UserStatus.DISABLED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
