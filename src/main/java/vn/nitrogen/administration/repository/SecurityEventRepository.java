package vn.nitrogen.administration.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.nitrogen.administration.domain.SecurityEvent;
import vn.nitrogen.administration.dto.SecurityEventType;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    List<SecurityEvent> findByCorrelationIdOrderByOccurredAtDesc(String correlationId);

    List<SecurityEvent> findByEventTypeOrderByOccurredAtDesc(
            SecurityEventType eventType,
            Pageable pageable);

    @Query("""
            select e
            from SecurityEvent e
            where e.actorId = :actorId
              and e.occurredAt >= :from
              and e.occurredAt <= :to
            order by e.occurredAt desc
            """)
    List<SecurityEvent> findByActor(
            @Param("actorId") UUID actorId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
