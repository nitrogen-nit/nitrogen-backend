package vn.nitrogen.administration.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nitrogen.administration.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
            String targetType,
            UUID targetId);

    @Query("""
            select a
            from AuditLog a
            where a.actorId = :actorId
              and a.occurredAt >= :from
              and a.occurredAt <= :to
            order by a.occurredAt desc
            """)
    List<AuditLog> findByActor(
            @Param("actorId") UUID actorId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
