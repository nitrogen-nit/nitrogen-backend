package vn.nitrogen.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import vn.nitrogen.identity.dto.RegisterUserRequest;
import vn.nitrogen.identity.dto.RegistrationAcceptedResponse;
import vn.nitrogen.identity.repository.EmailVerificationTokenRepository;
import vn.nitrogen.identity.repository.UserRepository;
import vn.nitrogen.integration.api.OutboxApi;
import vn.nitrogen.integration.dto.AppendOutboxCommand;

@Tag("unit")
class UserRegistrationServiceTest {

    private static final String IP = "203.0.113.7";
    private static final String USER_AGENT = "JUnit";

    private UserRepository users;
    private EmailVerificationTokenRepository verificationTokens;
    private PasswordEncoder passwordEncoder;
    private AuditApi audit;
    private SecurityEventApi securityEvents;
    private OutboxApi outbox;
    private TransactionTemplate transactions;
    private UserRegistrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        users = mock(UserRepository.class);
        verificationTokens = mock(EmailVerificationTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        audit = mock(AuditApi.class);
        securityEvents = mock(SecurityEventApi.class);
        outbox = mock(OutboxApi.class);
        transactions = mock(TransactionTemplate.class);

        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hashed");
        when(transactions.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
        when(outbox.append(any())).thenReturn(UUID.randomUUID());

        service = new UserRegistrationService(
                users,
                verificationTokens,
                passwordEncoder,
                new VerificationTokenGenerator(),
                audit,
                securityEvents,
                outbox,
                transactions,
                Duration.ofHours(24));
    }

    @Test
    void createsPendingUserWithHashedPasswordTokenAuditAndEvents() {
        when(users.findByEmailIgnoreCase("new@example.test")).thenReturn(Optional.empty());

        RegistrationAcceptedResponse response = service.register(
                new RegisterUserRequest("New@Example.test", "correct horse", " New User "), IP, USER_AGENT);

        assertThat(response).isEqualTo(RegistrationAcceptedResponse.verificationPending());

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(savedUser.capture());
        User user = savedUser.getValue();
        assertThat(user.getEmail()).isEqualTo("new@example.test");
        assertThat(user.getDisplayName()).isEqualTo("New User");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}hashed");

        ArgumentCaptor<EmailVerificationToken> savedToken = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokens).save(savedToken.capture());
        assertThat(savedToken.getValue().getUser()).isSameAs(user);
        assertThat(savedToken.getValue().getExpiresAt())
                .isEqualTo(savedToken.getValue().getIssuedAt().plus(Duration.ofHours(24)));

        verify(audit).record(user.getId(), "USER_REGISTERED", "USER", user.getId(), null);

        ArgumentCaptor<AppendOutboxCommand> events = ArgumentCaptor.forClass(AppendOutboxCommand.class);
        verify(outbox, times(2)).append(events.capture());
        List<AppendOutboxCommand> commands = events.getAllValues();
        assertThat(commands).extracting(AppendOutboxCommand::eventType)
                .containsExactly("identity.UserRegistered", "identity.EmailVerificationRequested");
        assertThat(commands.get(0).payload()).doesNotContainKeys("email", "verificationToken");
        String rawToken = (String) commands.get(1).payload().get("verificationToken");
        assertThat(new VerificationTokenGenerator().hash(rawToken)).isEqualTo(savedToken.getValue().getTokenHash());
        assertThat(rawToken).isNotEqualTo(savedToken.getValue().getTokenHash());

        verify(securityEvents).record(
                user.getId(),
                SecurityEventType.REGISTRATION_SUCCESS,
                SecurityEventSeverity.INFO,
                SecurityEventOutcome.SUCCESS,
                IP,
                USER_AGENT,
                null);
    }

    @Test
    void duplicateEmailGetsSameResponseAndOnlyLeavesSecurityEvent() {
        User existing = User.active("taken@example.test", "Taken");
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(users.findByEmailIgnoreCase("taken@example.test")).thenReturn(Optional.of(existing));

        RegistrationAcceptedResponse response = service.register(
                new RegisterUserRequest("taken@example.test", "correct horse", "Someone"), IP, USER_AGENT);

        assertThat(response).isEqualTo(RegistrationAcceptedResponse.verificationPending());
        // Băm cả ở nhánh trùng để thời gian phản hồi không lộ email đã tồn tại.
        verify(passwordEncoder).encode("correct horse");
        verify(users, never()).saveAndFlush(any());
        verify(verificationTokens, never()).save(any());
        verify(outbox, never()).append(any());
        verify(audit, never()).record(any(), anyString(), anyString(), any(), any());
        verify(securityEvents).record(
                eq(existing.getId()),
                eq(SecurityEventType.REGISTRATION_DUPLICATE_EMAIL),
                eq(SecurityEventSeverity.WARNING),
                eq(SecurityEventOutcome.BLOCKED),
                eq(IP),
                eq(USER_AGENT),
                anyString());
    }

    @Test
    void concurrentDuplicateCaughtByUniqueIndexIsTreatedAsDuplicate() {
        User winner = User.active("race@example.test", "Winner");
        ReflectionTestUtils.setField(winner, "id", UUID.randomUUID());
        doThrow(new DataIntegrityViolationException("uk_users_email")).when(transactions).execute(any());
        when(users.findByEmailIgnoreCase("race@example.test")).thenReturn(Optional.of(winner));

        RegistrationAcceptedResponse response = service.register(
                new RegisterUserRequest("race@example.test", "correct horse", "Loser"), IP, USER_AGENT);

        assertThat(response).isEqualTo(RegistrationAcceptedResponse.verificationPending());
        verify(securityEvents).record(
                eq(winner.getId()),
                eq(SecurityEventType.REGISTRATION_DUPLICATE_EMAIL),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void rejectsPasswordLongerThan72Utf8BytesBeforeHashing() {
        // 30 ký tự nhưng 90 byte UTF-8: qua được @Size, BCrypt sẽ cắt cụt.
        String password = "ệ".repeat(30);

        assertThatThrownBy(() -> service.register(
                new RegisterUserRequest("new@example.test", password, "New"), IP, USER_AGENT))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_POLICY_VIOLATION));

        verify(passwordEncoder, never()).encode(anyString());
        verify(securityEvents, never()).record(any(), any(), any(), any(), any(), any(), isNull());
    }
}
