package vn.nitrogen.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.nitrogen.administration.api.AuditApi;
import vn.nitrogen.administration.api.SecurityEventApi;
import vn.nitrogen.administration.dto.SecurityEventOutcome;
import vn.nitrogen.administration.dto.SecurityEventSeverity;
import vn.nitrogen.administration.dto.SecurityEventType;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;
import vn.nitrogen.identity.domain.EmailVerificationToken;
import vn.nitrogen.identity.domain.User;
import vn.nitrogen.identity.domain.UserStatus;
import vn.nitrogen.identity.repository.EmailVerificationTokenRepository;

@Tag("unit")
class EmailVerificationServiceTest {

    private static final String RAW_TOKEN = "raw-token";

    private final VerificationTokenGenerator tokenGenerator = new VerificationTokenGenerator();
    private EmailVerificationTokenRepository verificationTokens;
    private AuditApi audit;
    private SecurityEventApi securityEvents;
    private TransactionTemplate transactions;
    private EmailVerificationService service;
    private User user;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        verificationTokens = mock(EmailVerificationTokenRepository.class);
        audit = mock(AuditApi.class);
        securityEvents = mock(SecurityEventApi.class);
        transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));

        user = User.registerPending("new@example.test", "New", "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        service = new EmailVerificationService(verificationTokens, tokenGenerator, audit, securityEvents, transactions);
    }

    @Test
    void validTokenVerifiesEmailActivatesUserAndConsumesToken() {
        EmailVerificationToken token = tokenExpiringIn(3600);

        service.verify(RAW_TOKEN, "203.0.113.7", "JUnit");

        assertThat(token.isConsumed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(audit).record(user.getId(), "USER_EMAIL_VERIFIED", "USER", user.getId(), null);
        verify(securityEvents).record(
                user.getId(),
                SecurityEventType.EMAIL_VERIFICATION_SUCCESS,
                SecurityEventSeverity.INFO,
                SecurityEventOutcome.SUCCESS,
                "203.0.113.7",
                "JUnit",
                null);
    }

    @Test
    void unknownTokenFailsWithGenericError() {
        when(verificationTokens.findWithUserByTokenHash(anyString())).thenReturn(Optional.empty());

        assertInvalidToken();
        verifyFailureRecorded(null);
    }

    @Test
    void expiredTokenFailsWithSameError() {
        EmailVerificationToken token = tokenExpiringIn(-1);

        assertInvalidToken();
        assertThat(token.isConsumed()).isFalse();
        assertThat(user.isEmailVerified()).isFalse();
        verifyFailureRecorded(user.getId());
    }

    @Test
    void consumedTokenCannotBeReused() {
        EmailVerificationToken token = tokenExpiringIn(3600);
        token.consume(Instant.now());

        assertInvalidToken();
        verifyFailureRecorded(user.getId());
    }

    @Test
    void concurrentUseOfSameTokenFailsForLoser() {
        doThrow(new ObjectOptimisticLockingFailureException(EmailVerificationToken.class, UUID.randomUUID()))
                .when(transactions).execute(any());

        assertInvalidToken();
        verifyFailureRecorded(null);
    }

    private EmailVerificationToken tokenExpiringIn(long seconds) {
        Instant now = Instant.now();
        EmailVerificationToken token = EmailVerificationToken.issue(
                user,
                tokenGenerator.hash(RAW_TOKEN),
                now.minus(1, ChronoUnit.DAYS),
                now.plusSeconds(seconds));
        when(verificationTokens.findWithUserByTokenHash(tokenGenerator.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(token));
        return token;
    }

    private void assertInvalidToken() {
        assertThatThrownBy(() -> service.verify(RAW_TOKEN, null, null))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));
        verify(audit, never()).record(any(), anyString(), anyString(), any(), any());
    }

    private void verifyFailureRecorded(UUID actorId) {
        verify(securityEvents).record(
                eq(actorId),
                eq(SecurityEventType.EMAIL_VERIFICATION_FAILURE),
                eq(SecurityEventSeverity.WARNING),
                eq(SecurityEventOutcome.FAILURE),
                any(),
                any(),
                anyString());
    }
}
