package vn.nitrogen.identity.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
import vn.nitrogen.identity.dto.RegisterUserRequest;
import vn.nitrogen.identity.dto.RegistrationAcceptedResponse;
import vn.nitrogen.identity.repository.EmailVerificationTokenRepository;
import vn.nitrogen.identity.repository.UserRepository;
import vn.nitrogen.identity.service.VerificationTokenGenerator.GeneratedToken;
import vn.nitrogen.integration.api.OutboxApi;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.platform.observability.CorrelationId;

/**
 * Đăng ký tài khoản bằng email/password (NIT-20).
 *
 * <p>Không lộ email nào đã tồn tại: email mới hay email trùng đều nhận cùng
 * một phản hồi, và cả hai nhánh đều băm mật khẩu trước khi chạm DB để thời gian
 * phản hồi không khác nhau theo bậc độ lớn. Khác biệt chỉ ghi vào security
 * event, nơi chỉ quản trị đọc được.
 *
 * <p>Transaction mở bằng {@link TransactionTemplate} thay vì
 * {@code @Transactional} vì hai việc phải nằm NGOÀI transaction: băm mật khẩu
 * (chậm, không nên giữ connection) và ghi security event (sau commit, để
 * {@code REGISTRATION_SUCCESS} không tồn tại cho một đăng ký đã rollback).
 */
@Service
@Profile("core")
public class UserRegistrationService {

    static final String AUDIT_ACTION_REGISTERED = "USER_REGISTERED";
    static final String AUDIT_TARGET_USER = "USER";

    /** BCrypt chỉ dùng 72 byte đầu; dài hơn thì phần đuôi bị bỏ qua âm thầm. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenGenerator tokenGenerator;
    private final AuditApi audit;
    private final SecurityEventApi securityEvents;
    private final OutboxApi outbox;
    private final TransactionTemplate transactions;
    private final Duration verificationTtl;

    public UserRegistrationService(
            UserRepository users,
            EmailVerificationTokenRepository verificationTokens,
            PasswordEncoder passwordEncoder,
            VerificationTokenGenerator tokenGenerator,
            AuditApi audit,
            SecurityEventApi securityEvents,
            OutboxApi outbox,
            TransactionTemplate transactions,
            @Value("${nitrogen.identity.email-verification.ttl:PT24H}") Duration verificationTtl) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.audit = audit;
        this.securityEvents = securityEvents;
        this.outbox = outbox;
        this.transactions = transactions;
        this.verificationTtl = verificationTtl;
    }

    /**
     * {@code NEVER}: use case này tự chia transaction — xem javadoc của class.
     * Gọi từ trong một transaction khác sẽ phá cả hai lý do đó nên bị chặn luôn.
     */
    @Transactional(propagation = Propagation.NEVER)
    public RegistrationAcceptedResponse register(RegisterUserRequest request, String ipAddress, String userAgent) {
        requirePasswordWithinLimit(request.password());
        String passwordHash = passwordEncoder.encode(request.password());

        Registration registration;
        try {
            registration = transactions.execute(status -> registerInTransaction(request, passwordHash));
        } catch (DataIntegrityViolationException ex) {
            // Hai request cùng email chạy song song: cả hai qua được bước kiểm tra
            // tồn tại, unique index uk_users_email chặn request về sau. Với client,
            // đó chỉ là thêm một trường hợp email trùng.
            registration = Registration.duplicate(findUserId(request.email()));
        }

        recordSecurityEvent(registration, ipAddress, userAgent);
        return RegistrationAcceptedResponse.verificationPending();
    }

    private Registration registerInTransaction(RegisterUserRequest request, String passwordHash) {
        UUID existingUserId = findUserId(request.email());
        if (existingUserId != null) {
            return Registration.duplicate(existingUserId);
        }

        Instant now = Instant.now();
        User user = users.saveAndFlush(
                User.registerPending(request.email(), request.displayName().strip(), passwordHash));

        GeneratedToken token = tokenGenerator.generate();
        Instant expiresAt = now.plus(verificationTtl);
        verificationTokens.save(EmailVerificationToken.issue(user, token.hash(), now, expiresAt));

        audit.record(user.getId(), AUDIT_ACTION_REGISTERED, AUDIT_TARGET_USER, user.getId(), null);
        appendEvents(user, token, now, expiresAt);
        return Registration.created(user.getId());
    }

    private void appendEvents(User user, GeneratedToken token, Instant registeredAt, Instant expiresAt) {
        UUID correlationId = CorrelationId.currentUuidOrNew();

        Map<String, Object> registered = new LinkedHashMap<>();
        registered.put("userId", user.getId().toString());
        registered.put("registeredAt", registeredAt.toString());
        registered.put("emailVerified", false);
        UUID registeredEventId = outbox.append(new AppendOutboxCommand(
                IdentityEvents.USER_AGGREGATE,
                user.getId(),
                IdentityEvents.USER_REGISTERED,
                IdentityEvents.USER_REGISTERED_ROUTING_KEY,
                IdentityEvents.SCHEMA_VERSION,
                correlationId,
                null,
                registered));

        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("userId", user.getId().toString());
        verification.put("email", user.getEmail());
        verification.put("displayName", user.getDisplayName());
        verification.put("verificationToken", token.raw());
        verification.put("expiresAt", expiresAt.toString());
        outbox.append(new AppendOutboxCommand(
                IdentityEvents.USER_AGGREGATE,
                user.getId(),
                IdentityEvents.EMAIL_VERIFICATION_REQUESTED,
                IdentityEvents.EMAIL_VERIFICATION_REQUESTED_ROUTING_KEY,
                IdentityEvents.SCHEMA_VERSION,
                correlationId,
                registeredEventId,
                verification));
    }

    private void recordSecurityEvent(Registration registration, String ipAddress, String userAgent) {
        if (registration.created()) {
            securityEvents.record(
                    registration.userId(),
                    SecurityEventType.REGISTRATION_SUCCESS,
                    SecurityEventSeverity.INFO,
                    SecurityEventOutcome.SUCCESS,
                    ipAddress,
                    userAgent,
                    null);
        } else {
            securityEvents.record(
                    registration.userId(),
                    SecurityEventType.REGISTRATION_DUPLICATE_EMAIL,
                    SecurityEventSeverity.WARNING,
                    SecurityEventOutcome.BLOCKED,
                    ipAddress,
                    userAgent,
                    "Registration attempted for an existing account.");
        }
    }

    private UUID findUserId(String email) {
        return users.findByEmailIgnoreCase(email.strip()).map(User::getId).orElse(null);
    }

    /**
     * Độ dài theo ký tự đã do Bean Validation chặn; ở đây chặn theo byte, vì
     * 72 ký tự tiếng Việt có dấu có thể vượt xa 72 byte UTF-8.
     */
    private static void requirePasswordWithinLimit(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_POLICY_VIOLATION,
                    ErrorCode.PASSWORD_POLICY_VIOLATION.status(),
                    "Password must not exceed 72 bytes.");
        }
    }

    private record Registration(boolean created, UUID userId) {

        static Registration created(UUID userId) {
            return new Registration(true, userId);
        }

        static Registration duplicate(UUID existingUserId) {
            return new Registration(false, existingUserId);
        }
    }
}
