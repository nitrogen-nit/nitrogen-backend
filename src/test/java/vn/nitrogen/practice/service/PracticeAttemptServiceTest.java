package vn.nitrogen.practice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;
import vn.nitrogen.identity.api.IdentityQueryApi;
import vn.nitrogen.practice.domain.AttemptKind;
import vn.nitrogen.practice.domain.AttemptStatus;
import vn.nitrogen.practice.domain.OriginType;
import vn.nitrogen.practice.domain.PracticeAttempt;
import vn.nitrogen.practice.dto.PracticeAttemptSummary;
import vn.nitrogen.practice.dto.StartPracticeAttemptRequest;
import vn.nitrogen.practice.repository.PracticeAttemptRepository;

@Tag("unit")
class PracticeAttemptServiceTest {

    private StubPracticeAttemptRepository attempts;
    private StubIdentityQueryApi identity;
    private PracticeAttemptService service;

    @BeforeEach
    void setUp() {
        attempts = new StubPracticeAttemptRepository();
        identity = new StubIdentityQueryApi();
        service = new PracticeAttemptService(attempts.proxy(), identity);
    }

    @Test
    void startsAttemptForActiveUser() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        StartPracticeAttemptRequest request = request(userId, originId, null);

        identity.activeUsers.add(userId);
        attempts.count = 2L;

        PracticeAttemptSummary summary = service.start(request);

        assertThat(summary.userId()).isEqualTo(userId);
        assertThat(summary.originId()).isEqualTo(originId);
        assertThat(summary.attemptKind()).isEqualTo("TOPIC_PRACTICE");
        assertThat(summary.originType()).isEqualTo("CURRICULUM_NODE");
        assertThat(summary.attemptNo()).isEqualTo(3);
        assertThat(summary.status()).isEqualTo("IN_PROGRESS");
        assertThat(summary.gradingStatus()).isEqualTo("NOT_REQUIRED");
        assertThat(attempts.saveCalls).isEqualTo(1);
    }

    @Test
    void replaysExistingAttemptForSameIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        PracticeAttempt existing = attempt(userId, originId, 1, "same-command");

        identity.activeUsers.add(userId);
        attempts.idempotentResponses.add(Optional.of(existing));

        PracticeAttemptSummary summary = service.start(request(userId, originId, " same-command "));

        assertThat(summary.id()).isEqualTo(existing.getId());
        assertThat(attempts.saveCalls).isZero();
    }

    @Test
    void rejectsMissingOrInactiveUser() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.start(request(userId, UUID.randomUUID(), null)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void rejectsSecondActiveAttemptBeforeInsert() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        identity.activeUsers.add(userId);
        attempts.activeAttemptExists = true;

        assertThatThrownBy(() -> service.start(request(userId, originId, null)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ATTEMPT_ACTIVE_EXISTS));
        assertThat(attempts.saveCalls).isZero();
    }

    @Test
    void mapsInsertRaceToActiveAttemptConflict() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        StartPracticeAttemptRequest request = request(userId, originId, null);
        identity.activeUsers.add(userId);
        attempts.saveFailure = new DataIntegrityViolationException("duplicate");

        assertThatThrownBy(() -> service.start(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ATTEMPT_ACTIVE_EXISTS));
    }

    @Test
    void replaysIdempotentAttemptAfterInsertRace() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        PracticeAttempt existing = attempt(userId, originId, 1, "same-command");
        StartPracticeAttemptRequest request = request(userId, originId, "same-command");

        identity.activeUsers.add(userId);
        attempts.idempotentResponses.add(Optional.empty());
        attempts.idempotentResponses.add(Optional.of(existing));
        attempts.saveFailure = new DataIntegrityViolationException("duplicate");

        PracticeAttemptSummary summary = service.start(request);

        assertThat(summary.id()).isEqualTo(existing.getId());
    }

    @Test
    void rejectsUnsupportedAttemptKind() {
        StartPracticeAttemptRequest request = new StartPracticeAttemptRequest(
                UUID.randomUUID(),
                "DRILL",
                "CURRICULUM_NODE",
                UUID.randomUUID(),
                null,
                null);

        assertThatThrownBy(() -> service.start(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void returnsAttemptByIdOrNotFound() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        PracticeAttempt existing = attempt(userId, originId, 4, null);
        UUID missingId = UUID.nameUUIDFromBytes("missing".getBytes(StandardCharsets.UTF_8));
        attempts.attemptsById.put(existing.getId(), existing);

        assertThat(service.findById(existing.getId()).attemptNo()).isEqualTo(4);
        assertThatThrownBy(() -> service.findById(missingId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ATTEMPT_NOT_FOUND));
    }

    @Test
    void boundsRecentAttemptLimitAndCountsOrigin() {
        UUID userId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        attempts.recentAttempts =
                List.of(attempt(userId, originId, 1, null), attempt(userId, UUID.randomUUID(), 1, null));
        attempts.count = 7L;

        assertThat(service.findRecentByUser(userId, 0)).hasSize(1);
        assertThat(service.countByUserAndOrigin(userId, "CURRICULUM_NODE", originId)).isEqualTo(7L);
    }

    private static StartPracticeAttemptRequest request(UUID userId, UUID originId, String idempotencyKey) {
        return new StartPracticeAttemptRequest(
                userId,
                "TOPIC_PRACTICE",
                "CURRICULUM_NODE",
                originId,
                idempotencyKey,
                Instant.parse("2026-09-19T00:00:00Z"));
    }

    private static PracticeAttempt attempt(UUID userId, UUID originId, int attemptNo, String idempotencyKey) {
        return new PracticeAttempt(
                userId,
                AttemptKind.TOPIC_PRACTICE,
                OriginType.CURRICULUM_NODE,
                originId,
                attemptNo,
                idempotencyKey,
                null);
    }

    private static final class StubIdentityQueryApi extends IdentityQueryApi {

        private final Set<UUID> activeUsers = new HashSet<>();

        private StubIdentityQueryApi() {
            super(null);
        }

        @Override
        public boolean exists(UUID userId) {
            return activeUsers.contains(userId);
        }
    }

    private static final class StubPracticeAttemptRepository implements InvocationHandler {

        private boolean activeAttemptExists;
        private long count;
        private final ArrayDeque<Optional<PracticeAttempt>> idempotentResponses = new ArrayDeque<>();
        private final Map<UUID, PracticeAttempt> attemptsById = new HashMap<>();
        private List<PracticeAttempt> recentAttempts = List.of();
        private RuntimeException saveFailure;
        private int saveCalls;

        private PracticeAttemptRepository proxy() {
            return (PracticeAttemptRepository) Proxy.newProxyInstance(
                    PracticeAttemptRepository.class.getClassLoader(),
                    new Class<?>[] {PracticeAttemptRepository.class},
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByUserIdAndOriginTypeAndOriginIdAndStatus" -> activeAttemptExists;
                case "countByUserIdAndOriginTypeAndOriginId" -> count;
                case "findByUserIdAndIdempotencyKey" -> idempotentResponses.isEmpty()
                        ? Optional.empty()
                        : idempotentResponses.removeFirst();
                case "findById" -> Optional.ofNullable(attemptsById.get(args[0]));
                case "findTop20ByUserIdOrderByStartedAtDesc" -> recentAttempts;
                case "saveAndFlush" -> save((PracticeAttempt) args[0]);
                case "toString" -> "StubPracticeAttemptRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }

        private PracticeAttempt save(PracticeAttempt attempt) {
            saveCalls++;
            if (saveFailure != null) {
                throw saveFailure;
            }
            return attempt;
        }
    }
}
