package vn.nitrogen.practice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nitrogen.practice.domain.AttemptStatus;
import vn.nitrogen.practice.domain.OriginType;
import vn.nitrogen.practice.domain.PracticeAttempt;

public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt, UUID> {

    boolean existsByUserIdAndOriginTypeAndOriginIdAndStatus(
            UUID userId, OriginType originType, UUID originId, AttemptStatus status);

    long countByUserIdAndOriginTypeAndOriginId(UUID userId, OriginType originType, UUID originId);

    Optional<PracticeAttempt> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    List<PracticeAttempt> findTop20ByUserIdOrderByStartedAtDesc(UUID userId);
}
