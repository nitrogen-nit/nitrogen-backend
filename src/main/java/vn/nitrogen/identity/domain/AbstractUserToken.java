package vn.nitrogen.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import vn.nitrogen.common.domain.AbstractIdentifiableEntity;

/**
 * Phần chung của mọi token cấp cho một {@link User} — refresh token và
 * password reset token.
 *
 * <p>Hai bảng {@code identity.refresh_tokens} và
 * {@code identity.password_reset_tokens} có cùng bộ cột
 * ({@code user_id}, {@code token_hash}, {@code issued_at}, {@code expires_at},
 * {@code row_version}); chỉ cột đánh dấu vòng đời là khác
 * ({@code revoked_at} so với {@code consumed_at}). Phần giống nhau sống ở đây,
 * phần khác nhau do lớp con tự mang.
 *
 * <p>Đây là {@code @MappedSuperclass} chứ không phải {@code @Entity}: mỗi lớp
 * con vẫn ánh xạ vào bảng riêng của nó, không có bảng nào cho lớp này và
 * không sinh thêm join nào.
 *
 * <p>Token là immutable sau khi phát hành — không có setter. Trạng thái duy
 * nhất được phép đổi là dấu vô hiệu hoá, do lớp con mở ra bằng một phương
 * thức nghiệp vụ ({@code revoke}/{@code consume}).
 */
@MappedSuperclass
public abstract class AbstractUserToken extends AbstractIdentifiableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected AbstractUserToken() {
    }

    protected AbstractUserToken(User user, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
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

    public long getRowVersion() {
        return rowVersion;
    }

    /**
     * Token đã quá hạn tại thời điểm {@code at} hay chưa.
     *
     * <p>Độc lập với dấu vô hiệu hoá của lớp con: một token có thể vừa hết hạn
     * vừa chưa bị thu hồi.
     */
    public boolean isExpiredAt(Instant at) {
        return !expiresAt.isAfter(at);
    }
}
