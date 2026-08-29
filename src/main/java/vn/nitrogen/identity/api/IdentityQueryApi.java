package vn.nitrogen.identity.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.identity.dto.UserSummary;

/**
 * Tra cứu user cho module khác (§15.1: entity chỉ giữ {@code UUID userId},
 * gọi API này khi cần thông tin nghiệp vụ).
 *
 * <p>TODO: inject {@code UserRepository} nội bộ module và hiện thực.
 */
@Profile("core")
@Controller
@Lazy
public class IdentityQueryApi implements ModuleApi {

    public Optional<UserSummary> findById(UUID userId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực IdentityQueryApi#findById");
    }

    /** Tra hàng loạt trong một query — tránh N+1 khi render danh sách attempt. */
    public List<UserSummary> findAllById(Collection<UUID> userIds) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực IdentityQueryApi#findAllById");
    }

    public boolean exists(UUID userId) {
        throw new UnsupportedOperationException("TODO: chưa hiện thực IdentityQueryApi#exists");
    }
}
