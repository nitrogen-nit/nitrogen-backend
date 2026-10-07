package vn.nitrogen.integration.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.domain.OutboxStatus;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByOccurredAtDescIdDesc(
            OutboxStatus status,
            Pageable pageable);
}
