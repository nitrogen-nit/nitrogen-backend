package vn.nitrogen.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Luật nghiệp vụ của các entity trong module identity.
 *
 * <p>Thuần POJO, không Spring context và không Docker: những luật ở đây không
 * cần database để kiểm chứng, nên chúng chạy trong profile {@code unit} và
 * không phụ thuộc Testcontainers.
 */
@Tag("unit")
class IdentityDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-05T00:00:00Z");

    private static User newUser() {
        return User.active("learner@example.test", "Learner");
    }

    @Nested
    class UserRules {

        @Test
        void normalizesEmailOnCreation() {
            User user = User.active("  MiXeD.Case@Example.TEST  ", "Mixed Case");

            assertThat(user.getEmail()).isEqualTo("mixed.case@example.test");
        }

        @Test
        void startsActiveWithDefaultTimezoneAndNoCredentials() {
            User user = newUser();

            assertThat(user.isActive()).isTrue();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
            assertThat(user.getPasswordHash()).isNull();
            assertThat(user.isEmailVerified()).isFalse();
            assertThat(user.getEmailVerifiedAt()).isNull();
            assertThat(user.getLastLoginAt()).isNull();
            assertThat(user.getRoles()).isEmpty();
            assertThat(user.getRowVersion()).isZero();
            assertThat(user.getDisplayName()).isEqualTo("Learner");
        }

        @Test
        void disableMakesUserInactive() {
            User user = newUser();

            user.disable();

            assertThat(user.isActive()).isFalse();
            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        }

        @Test
        void changePasswordStoresHash() {
            User user = newUser();

            user.changePassword("argon2id$hash");

            assertThat(user.getPasswordHash()).isEqualTo("argon2id$hash");
        }

        @Test
        void verifyEmailSetsFlagAndTimestampTogether() {
            User user = newUser();

            user.verifyEmail(NOW);

            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        }

        @Test
        void registerPendingStoresHashAndWaitsForVerification() {
            User user = User.registerPending("  New@Example.TEST ", "New", "{bcrypt}hash");

            assertThat(user.getEmail()).isEqualTo("new@example.test");
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(user.isActive()).isFalse();
            assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}hash");
            assertThat(user.isEmailVerified()).isFalse();
        }

        @Test
        void verifyEmailActivatesPendingUser() {
            User user = User.registerPending("new@example.test", "New", "{bcrypt}hash");

            user.verifyEmail(NOW);

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        }

        @Test
        void verifyEmailDoesNotReactivateDisabledUser() {
            User user = User.registerPending("new@example.test", "New", "{bcrypt}hash");
            user.disable();

            user.verifyEmail(NOW);

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(user.isEmailVerified()).isTrue();
        }

        @Test
        void recordLoginStoresLastLoginInstant() {
            User user = newUser();

            user.recordLogin(NOW);

            assertThat(user.getLastLoginAt()).isEqualTo(NOW);
        }

        @Test
        void setTimezoneOverridesDefault() {
            User user = newUser();

            user.setTimezone("UTC");

            assertThat(user.getTimezone()).isEqualTo("UTC");
        }

        @Test
        void addRoleAccumulatesDistinctRoles() {
            User user = newUser();

            user.addRole("LEARNER");
            user.addRole("ADMIN");
            user.addRole("LEARNER");

            assertThat(user.getRoles()).containsExactlyInAnyOrder("LEARNER", "ADMIN");
        }

        @Test
        void exposesRolesAsImmutableCopy() {
            User user = newUser();
            user.addRole("LEARNER");

            assertThatThrownBy(() -> user.getRoles().add("ADMIN"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(user.getRoles()).containsExactly("LEARNER");
        }

        @Test
        void populatesTimestampsOnPersist() {
            User user = newUser();

            user.onCreate();

            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
        }

        @Test
        void advancesUpdatedAtOnUpdate() {
            User user = newUser();
            user.onCreate();
            Instant createdAt = user.getCreatedAt();

            user.onUpdate();

            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
            assertThat(user.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        }
    }

    @Nested
    class RefreshTokenRules {

        private RefreshToken issued() {
            return RefreshToken.issue(newUser(), "refresh-hash", NOW, NOW.plusSeconds(3600));
        }

        @Test
        void carriesIssuedStateFromFactory() {
            User user = newUser();

            RefreshToken token = RefreshToken.issue(user, "refresh-hash", NOW, NOW.plusSeconds(3600));

            assertThat(token.getUser()).isSameAs(user);
            assertThat(token.getTokenHash()).isEqualTo("refresh-hash");
            assertThat(token.getIssuedAt()).isEqualTo(NOW);
            assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
            assertThat(token.getRowVersion()).isZero();
        }

        @Test
        void startsNotRevoked() {
            RefreshToken token = issued();

            assertThat(token.isRevoked()).isFalse();
            assertThat(token.getRevokedAt()).isNull();
        }

        @Test
        void revokeMarksTokenWithInstant() {
            RefreshToken token = issued();

            token.revoke(NOW.plusSeconds(60));

            assertThat(token.isRevoked()).isTrue();
            assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(60));
        }

        @Test
        void expiryIsIndependentOfRevocation() {
            RefreshToken token = issued();

            assertThat(token.isExpiredAt(NOW.plusSeconds(3599))).isFalse();
            assertThat(token.isExpiredAt(NOW.plusSeconds(3600))).isTrue();
            assertThat(token.isExpiredAt(NOW.plusSeconds(3601))).isTrue();
            assertThat(token.isRevoked()).isFalse();
        }
    }

    @Nested
    class PasswordResetTokenRules {

        private PasswordResetToken issued() {
            return PasswordResetToken.issue(newUser(), "reset-hash", NOW, NOW.plusSeconds(900));
        }

        @Test
        void carriesIssuedStateFromFactory() {
            User user = newUser();

            PasswordResetToken token =
                    PasswordResetToken.issue(user, "reset-hash", NOW, NOW.plusSeconds(900));

            assertThat(token.getUser()).isSameAs(user);
            assertThat(token.getTokenHash()).isEqualTo("reset-hash");
            assertThat(token.getIssuedAt()).isEqualTo(NOW);
            assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
            assertThat(token.getRowVersion()).isZero();
        }

        @Test
        void startsNotConsumed() {
            PasswordResetToken token = issued();

            assertThat(token.isConsumed()).isFalse();
            assertThat(token.getConsumedAt()).isNull();
        }

        @Test
        void consumeMarksTokenWithInstant() {
            PasswordResetToken token = issued();

            token.consume(NOW.plusSeconds(30));

            assertThat(token.isConsumed()).isTrue();
            assertThat(token.getConsumedAt()).isEqualTo(NOW.plusSeconds(30));
        }

        @Test
        void expiryIsIndependentOfConsumption() {
            PasswordResetToken token = issued();

            assertThat(token.isExpiredAt(NOW.plusSeconds(899))).isFalse();
            assertThat(token.isExpiredAt(NOW.plusSeconds(900))).isTrue();
            assertThat(token.isConsumed()).isFalse();
        }
    }

    @Nested
    class OAuthAccountRules {

        @Test
        void carriesLinkedStateFromFactory() {
            User user = newUser();

            OAuthAccount account = OAuthAccount.link(user, "google", "google-sub-1", user.getEmail());

            assertThat(account.getUser()).isSameAs(user);
            assertThat(account.getProvider()).isEqualTo("google");
            assertThat(account.getProviderSubject()).isEqualTo("google-sub-1");
            assertThat(account.getProviderEmail()).isEqualTo("learner@example.test");
            assertThat(account.getRowVersion()).isZero();
        }

        @Test
        void allowsMissingProviderEmail() {
            OAuthAccount account = OAuthAccount.link(newUser(), "apple", "apple-sub-1", null);

            assertThat(account.getProviderEmail()).isNull();
        }

        @Test
        void populatesTimestampsOnPersist() {
            OAuthAccount account =
                    OAuthAccount.link(newUser(), "google", "google-sub-2", "learner@example.test");

            account.onCreate();

            assertThat(account.getLinkedAt()).isNotNull();
            assertThat(account.getUpdatedAt()).isEqualTo(account.getLinkedAt());
        }

        @Test
        void advancesUpdatedAtOnUpdateAndKeepsLinkedAt() {
            OAuthAccount account =
                    OAuthAccount.link(newUser(), "google", "google-sub-3", "learner@example.test");
            account.onCreate();
            Instant linkedAt = account.getLinkedAt();

            account.onUpdate();

            assertThat(account.getLinkedAt()).isEqualTo(linkedAt);
            assertThat(account.getUpdatedAt()).isAfterOrEqualTo(linkedAt);
        }
    }

    @Nested
    class EmailVerificationTokenRules {

        @Test
        void consumeMarksTokenUsed() {
            EmailVerificationToken token = EmailVerificationToken.issue(
                    newUser(), "hash", NOW, NOW.plusSeconds(60));

            assertThat(token.isConsumed()).isFalse();
            token.consume(NOW.plusSeconds(1));

            assertThat(token.isConsumed()).isTrue();
            assertThat(token.getConsumedAt()).isEqualTo(NOW.plusSeconds(1));
        }

        @Test
        void expiresAtExpiryInstant() {
            EmailVerificationToken token = EmailVerificationToken.issue(
                    newUser(), "hash", NOW, NOW.plusSeconds(60));

            assertThat(token.isExpiredAt(NOW.plusSeconds(59))).isFalse();
            assertThat(token.isExpiredAt(NOW.plusSeconds(60))).isTrue();
        }
    }
}
