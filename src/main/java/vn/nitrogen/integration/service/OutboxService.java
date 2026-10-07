package vn.nitrogen.integration.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.domain.OutboxStatus;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.repository.OutboxEventRepository;

@Service
@Transactional(readOnly = true)
public class OutboxService {

    private static final int MAX_LIMIT = 100;

    private final OutboxEventRepository outboxEvents;

    public OutboxService(OutboxEventRepository outboxEvents) {
        this.outboxEvents = outboxEvents;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID append(AppendOutboxCommand command) {
        Objects.requireNonNull(command, "command");

        OutboxEvent event = OutboxEvent.pending(
                command.aggregateType(),
                command.aggregateId(),
                command.eventType(),
                command.routingKey(),
                command.schemaVersion(),
                command.correlationId(),
                command.causationId(),
                command.payload(),
                Instant.now());

        outboxEvents.save(event);
        return event.getId();
    }

    public List<OutboxEvent> findFailed(int limit) {
        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);

        return outboxEvents.findByStatusOrderByOccurredAtDescIdDesc(
                OutboxStatus.FAILED,
                PageRequest.of(0, safeLimit));
    }
}
