package vn.nitrogen.identity.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
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
import vn.nitrogen.identity.repository.EmailVerificationTokenRepository;

/**
 * Xác minh email bằng token một lần được gửi sau đăng ký.
 *
 * <p>Token không tồn tại, đã dùng hay đã hết hạn đều trả cùng một lỗi: phân
 * biệt chúng chỉ giúp người dò token, không giúp người dùng thật. Lý do cụ thể
 * nằm trong security event.
 *
 * <p>Transaction mở bằng {@link TransactionTemplate} để security event ghi
 * sau khi transaction kết thúc (cùng lý do như {@link UserRegistrationService})
 * và để xung đột {@code row_version} khi commit được bắt như token đã dùng.
 */
@Service
@Profile("core")
public class EmailVerificationService {

    static final String AUDIT_ACTION_EMAIL_VERIFIED = "USER_EMAIL_VERIFIED";

    private final EmailVerificationTokenRepository verificationTokens;
    private final VerificationTokenGenerator tokenGenerator;
    private final AuditApi audit;
    private final SecurityEventApi securityEvents;
    private final TransactionTemplate transactions;

    public EmailVerificationService(
            EmailVerificationTokenRepository verificationTokens,
            VerificationTokenGenerator tokenGenerator,
            AuditApi audit,
            SecurityEventApi securityEvents,
            TransactionTemplate transactions) {
        this.verificationTokens = verificationTokens;
        this.tokenGenerator = tokenGenerator;
        this.audit = audit;
        this.securityEvents = securityEvents;
        this.transactions = transactions;
    }

    /**
     * {@code NEVER}: use case này tự chia transaction — xem javadoc của class.
     * Gọi từ trong một transaction khác sẽ phá cả hai lý do đó nên bị chặn luôn.
     */
    @Transactional(propagation = Propagation.NEVER)
    public void verify(String rawToken, String ipAddress, String userAgent) {
        Verification verification;
        try {
            verification = transactions.execute(status -> verifyInTransaction(rawToken));
        } catch (OptimisticLockingFailureException ex) {
            // Cùng một link được mở hai lần song song: row_version của token cho
            // một request thắng, request còn lại xử lý như token đã dùng.
            verification = Verification.failed(null, "Verification token already used.");
        }

        if (verification.succeeded()) {
            securityEvents.record(
                    verification.userId(),
                    SecurityEventType.EMAIL_VERIFICATION_SUCCESS,
                    SecurityEventSeverity.INFO,
                    SecurityEventOutcome.SUCCESS,
                    ipAddress,
                    userAgent,
                    null);
            return;
        }

        securityEvents.record(
                verification.userId(),
                SecurityEventType.EMAIL_VERIFICATION_FAILURE,
                SecurityEventSeverity.WARNING,
                SecurityEventOutcome.FAILURE,
                ipAddress,
                userAgent,
                verification.failureReason());
        throw new BusinessException(
                ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID,
                ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID.status(),
                "Verification token is invalid or has expired.");
    }

    private Verification verifyInTransaction(String rawToken) {
        Optional<EmailVerificationToken> found =
                verificationTokens.findWithUserByTokenHash(tokenGenerator.hash(rawToken.strip()));
        if (found.isEmpty()) {
            return Verification.failed(null, "Unknown verification token.");
        }

        EmailVerificationToken token = found.get();
        User user = token.getUser();
        Instant now = Instant.now();
        if (token.isConsumed()) {
            return Verification.failed(user.getId(), "Verification token already used.");
        }
        if (token.isExpiredAt(now)) {
            return Verification.failed(user.getId(), "Verification token expired.");
        }

        token.consume(now);
        if (!user.isEmailVerified()) {
            user.verifyEmail(now);
            audit.record(user.getId(), AUDIT_ACTION_EMAIL_VERIFIED,
                    UserRegistrationService.AUDIT_TARGET_USER, user.getId(), null);
        }
        return Verification.succeeded(user.getId());
    }

    private record Verification(boolean succeeded, UUID userId, String failureReason) {

        static Verification succeeded(UUID userId) {
            return new Verification(true, userId, null);
        }

        static Verification failed(UUID userId, String reason) {
            return new Verification(false, userId, reason);
        }
    }
}
