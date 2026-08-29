package vn.nitrogen.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;
import vn.nitrogen.common.util.UuidV7;

/**
 * Base entity cho aggregate có khoá chính UUID.
 *
 * <p>ID sinh tại application bằng UUIDv7 (§7) — KHÔNG dùng
 * {@code @GeneratedValue}: khoá phải có sẵn trước khi flush để outbox và
 * batch insert hoạt động.
 *
 * <p>{@code equals}/{@code hashCode} dựa trên id và cố tình không đụng tới
 * association nào (§15: không kéo lazy association qua equals/hashCode/toString).
 */
@MappedSuperclass
public abstract class AbstractIdentifiableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7.randomUuid();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractIdentifiableEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
