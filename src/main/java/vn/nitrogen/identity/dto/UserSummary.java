package vn.nitrogen.identity.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Thông tin tối thiểu về một user mà module khác được phép biết.
 *
 * <p>Cố tình không có email/password/token: module khác không có nhu cầu và
 * không nên có khả năng đọc chúng.
 */
public record UserSummary(UUID id, String displayName, Set<String> roles, boolean active) {
}
