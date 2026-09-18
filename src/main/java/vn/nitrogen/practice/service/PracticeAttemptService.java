package vn.nitrogen.practice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;
import vn.nitrogen.identity.api.IdentityQueryApi;
import vn.nitrogen.practice.domain.AttemptKind;
import vn.nitrogen.practice.domain.AttemptStatus;
import vn.nitrogen.practice.domain.GradingStatus;
import vn.nitrogen.practice.domain.OriginType;
import vn.nitrogen.practice.domain.PracticeAttempt;
import vn.nitrogen.practice.dto.PracticeAttemptSummary;
import vn.nitrogen.practice.dto.StartPracticeAttemptRequest;
import vn.nitrogen.practice.repository.PracticeAttemptRepository;

@Service
@Profile("core")
@Transactional(readOnly = true)
public class PracticeAttemptService {

    private final PracticeAttemptRepository attempts;
    private final IdentityQueryApi identity;

    public PracticeAttemptService(PracticeAttemptRepository attempts, IdentityQueryApi identity) {
        this.attempts = attempts;
        this.identity = identity;
    }

    @Transactional
    public PracticeAttemptSummary start(StartPracticeAttemptRequest request) {
        AttemptKind attemptKind = parseEnum(AttemptKind.class, request.attemptKind(), "attemptKind");
        OriginType originType = parseEnum(OriginType.class, request.originType(), "originType");

        if (!identity.exists(request.userId())) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.USER_NOT_FOUND.status(),
                    "User does not exist or is not active.");
        }

        Optional<PracticeAttempt> idempotentAttempt = findExistingIdempotentAttempt(request);
        if (idempotentAttempt.isPresent()) {
            return toSummary(idempotentAttempt.get());
        }

        boolean activeAttemptExists = attempts.existsByUserIdAndOriginTypeAndOriginIdAndStatus(
                request.userId(), originType, request.originId(), AttemptStatus.IN_PROGRESS);
        if (activeAttemptExists) {
            throw new BusinessException(
                    ErrorCode.ATTEMPT_ACTIVE_EXISTS,
                    ErrorCode.ATTEMPT_ACTIVE_EXISTS.status(),
                    "An active attempt already exists for this origin.");
        }

        int attemptNo = Math.toIntExact(attempts.countByUserIdAndOriginTypeAndOriginId(
                request.userId(), originType, request.originId()) + 1);
        PracticeAttempt attempt = new PracticeAttempt(
                request.userId(),
                attemptKind,
                originType,
                request.originId(),
                attemptNo,
                normalizeIdempotencyKey(request.idempotencyKey()),
                request.deadlineAt());

        try {
            return toSummary(attempts.saveAndFlush(attempt));
        } catch (DataIntegrityViolationException ex) {
            Optional<PracticeAttempt> replay = findExistingIdempotentAttempt(request);
            if (replay.isPresent()) {
                return toSummary(replay.get());
            }
            throw new BusinessException(
                    ErrorCode.ATTEMPT_ACTIVE_EXISTS,
                    ErrorCode.ATTEMPT_ACTIVE_EXISTS.status(),
                    "An active attempt already exists for this origin.");
        }
    }

    public PracticeAttemptSummary findById(UUID attemptId) {
        return findOptionalById(attemptId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ATTEMPT_NOT_FOUND,
                        ErrorCode.ATTEMPT_NOT_FOUND.status(),
                        "Attempt was not found."));
    }

    public Optional<PracticeAttemptSummary> findOptionalById(UUID attemptId) {
        return attempts.findById(attemptId)
                .map(PracticeAttemptService::toSummary);
    }

    public List<PracticeAttemptSummary> findRecentByUser(UUID userId, int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 20);
        return attempts.findTop20ByUserIdOrderByStartedAtDesc(userId).stream()
                .limit(boundedLimit)
                .map(PracticeAttemptService::toSummary)
                .toList();
    }

    public long countByUserAndOrigin(UUID userId, String originType, UUID originId) {
        OriginType parsedOriginType = parseEnum(OriginType.class, originType, "originType");
        return attempts.countByUserIdAndOriginTypeAndOriginId(userId, parsedOriginType, originId);
    }

    private Optional<PracticeAttempt> findExistingIdempotentAttempt(StartPracticeAttemptRequest request) {
        String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return attempts.findByUserIdAndIdempotencyKey(request.userId(), idempotencyKey);
    }

    private static PracticeAttemptSummary toSummary(PracticeAttempt attempt) {
        return new PracticeAttemptSummary(
                attempt.getId(),
                attempt.getUserId(),
                attempt.getAttemptKind().name(),
                attempt.getOriginType().name(),
                attempt.getOriginId(),
                attempt.getAttemptNo(),
                attempt.getStatus().name(),
                gradingStatusName(attempt.getGradingStatus()),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.isScoreFinal(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getCompletedAt());
    }

    private static String gradingStatusName(GradingStatus gradingStatus) {
        return gradingStatus.name();
    }

    private static String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.strip();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is required.");
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is not supported: " + value);
        }
    }
}
