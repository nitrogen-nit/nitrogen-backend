package vn.nitrogen.identity.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import vn.nitrogen.common.api.ModuleApi;
import vn.nitrogen.identity.domain.User;
import vn.nitrogen.identity.dto.UserSummary;
import vn.nitrogen.identity.repository.UserRepository;

/**
 * Tra cứu user cho module khác (§15.1: entity chỉ giữ {@code UUID userId},
 * gọi API này khi cần thông tin nghiệp vụ).
 *
 * <p>Chỉ trả về user còn active. Module khác không được dùng API này để đọc
 * tài khoản đã khoá hoặc vô hiệu hoá.
 */
@Profile("core")
@Controller
public class IdentityQueryApi implements ModuleApi {

    private final UserRepository users;

    public IdentityQueryApi(UserRepository users) {
        this.users = users;
    }

    public Optional<UserSummary> findById(UUID userId) {
        return users.findById(userId)
                .filter(User::isActive)
                .map(IdentityQueryApi::toSummary);
    }

    /** Tra hàng loạt trong một query — tránh N+1 khi render danh sách attempt. */
    public List<UserSummary> findAllById(Collection<UUID> userIds) {
        return users.findAllByIdIn(userIds).stream()
                .filter(User::isActive)
                .map(IdentityQueryApi::toSummary)
                .toList();
    }

    public boolean exists(UUID userId) {
        return findById(userId).isPresent();
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getDisplayName(), Set.of(), user.isActive());
    }
}
