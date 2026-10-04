package vn.nitrogen.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import vn.nitrogen.identity.api.IdentityQueryApi;
import vn.nitrogen.identity.domain.OAuthAccount;
import vn.nitrogen.identity.domain.PasswordResetToken;
import vn.nitrogen.identity.domain.RefreshToken;
import vn.nitrogen.identity.domain.User;
import vn.nitrogen.identity.dto.UserSummary;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@ActiveProfiles({"test", "core"})
@Tag("docker")
class IdentityRepositoryTest extends TestcontainersBase {

    @Autowired
    private UserRepository users;

    @Autowired
    private OAuthAccountRepository oauthAccounts;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokens;

    @Autowired
    private IdentityQueryApi identity;

    @Test
    void findsUserByEmailIgnoringCase() {
        User user = users.saveAndFlush(User.active(uniqueEmail("email"), "Email User"));

        assertThat(users.findByEmailIgnoreCase(user.getEmail().toUpperCase()))
                .isPresent()
                .get()
                .extracting(User::getId)
                .isEqualTo(user.getId());
    }

    @Test
    void rejectsDuplicateEmailWithDifferentCase() {
        String email = uniqueEmail("duplicate");
        users.saveAndFlush(User.active(email, "First User"));

        assertThatThrownBy(() -> users.saveAndFlush(User.active(email.toUpperCase(), "Second User")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fetchesRolesAndTimezoneForActiveUserSummaries() {
        User user = User.active(uniqueEmail("roles"), "Role User");
        user.setTimezone("Asia/Ho_Chi_Minh");
        user.addRole("LEARNER");
        user.addRole("ADMIN");
        users.saveAndFlush(user);

        UserSummary summary = identity.findById(user.getId()).orElseThrow();

        assertThat(summary.displayName()).isEqualTo("Role User");
        assertThat(summary.timezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(summary.roles()).containsExactlyInAnyOrder("LEARNER", "ADMIN");
    }

    @Test
    void filtersInactiveUsersFromIdentityQueryApi() {
        User active = users.saveAndFlush(User.active(uniqueEmail("active"), "Active User"));
        User disabled = User.active(uniqueEmail("disabled"), "Disabled User");
        disabled.disable();
        users.saveAndFlush(disabled);

        assertThat(identity.findById(disabled.getId())).isEmpty();
        assertThat(identity.findAllById(List.of(active.getId(), disabled.getId())))
                .extracting(UserSummary::id)
                .containsExactly(active.getId());
    }

    @Test
    void findsOAuthAccountByProviderAndSubject() {
        User user = users.saveAndFlush(User.active(uniqueEmail("oauth"), "OAuth User"));
        OAuthAccount account = OAuthAccount.link(
                user,
                "google",
                "google-sub-%s".formatted(UUID.randomUUID()),
                user.getEmail());
        oauthAccounts.saveAndFlush(account);

        assertThat(oauthAccounts.findByProviderAndProviderSubject(
                account.getProvider(),
                account.getProviderSubject())).isPresent();
    }

    @Test
    void rejectsDuplicateOAuthProviderSubject() {
        User firstUser = users.saveAndFlush(User.active(uniqueEmail("oauth-first"), "First OAuth User"));
        User secondUser = users.saveAndFlush(User.active(uniqueEmail("oauth-second"), "Second OAuth User"));
        String providerSubject = "duplicate-sub-%s".formatted(UUID.randomUUID());
        oauthAccounts.saveAndFlush(OAuthAccount.link(firstUser, "google", providerSubject, firstUser.getEmail()));

        assertThatThrownBy(() -> oauthAccounts.saveAndFlush(
                OAuthAccount.link(secondUser, "google", providerSubject, secondUser.getEmail())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyActiveRefreshToken() {
        User user = users.saveAndFlush(User.active(uniqueEmail("refresh"), "Refresh User"));
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        RefreshToken token = refreshTokens.saveAndFlush(RefreshToken.issue(
                user,
                "refresh-hash-%s".formatted(UUID.randomUUID()),
                now.minusSeconds(60),
                now.plusSeconds(3600)));

        assertThat(refreshTokens.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                token.getTokenHash(),
                now)).isPresent();

        token.revoke(now);
        refreshTokens.saveAndFlush(token);

        assertThat(refreshTokens.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                token.getTokenHash(),
                now)).isEmpty();
    }

    @Test
    void rejectsDuplicateRefreshTokenHash() {
        User user = users.saveAndFlush(User.active(uniqueEmail("refresh-duplicate"), "Refresh Duplicate User"));
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        String tokenHash = "refresh-duplicate-%s".formatted(UUID.randomUUID());
        refreshTokens.saveAndFlush(RefreshToken.issue(
                user,
                tokenHash,
                now.minusSeconds(60),
                now.plusSeconds(3600)));

        assertThatThrownBy(() -> refreshTokens.saveAndFlush(RefreshToken.issue(
                user,
                tokenHash,
                now.minusSeconds(30),
                now.plusSeconds(7200))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyUsablePasswordResetToken() {
        User user = users.saveAndFlush(User.active(uniqueEmail("reset"), "Reset User"));
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        PasswordResetToken token = passwordResetTokens.saveAndFlush(PasswordResetToken.issue(
                user,
                "reset-hash-%s".formatted(UUID.randomUUID()),
                now.minusSeconds(60),
                now.plusSeconds(900)));

        assertThat(passwordResetTokens.findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
                token.getTokenHash(),
                now)).isPresent();

        token.consume(now);
        passwordResetTokens.saveAndFlush(token);

        assertThat(passwordResetTokens.findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
                token.getTokenHash(),
                now)).isEmpty();
    }

    @Test
    void rejectsDuplicatePasswordResetTokenHash() {
        User user = users.saveAndFlush(User.active(uniqueEmail("reset-duplicate"), "Reset Duplicate User"));
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        String tokenHash = "reset-duplicate-%s".formatted(UUID.randomUUID());
        passwordResetTokens.saveAndFlush(PasswordResetToken.issue(
                user,
                tokenHash,
                now.minusSeconds(60),
                now.plusSeconds(900)));

        assertThatThrownBy(() -> passwordResetTokens.saveAndFlush(PasswordResetToken.issue(
                user,
                tokenHash,
                now.minusSeconds(30),
                now.plusSeconds(1200))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String uniqueEmail(String prefix) {
        return "%s-%s@example.test".formatted(prefix, UUID.randomUUID());
    }
}
