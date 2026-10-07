package vn.nitrogen.integration.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nitrogen.integration.domain.OutboxEvent;
import vn.nitrogen.integration.domain.OutboxStatus;
import vn.nitrogen.integration.dto.AppendOutboxCommand;
import vn.nitrogen.integration.dto.ClaimedOutboxEvent;
import vn.nitrogen.integration.repository.OutboxEventRepository;

@Service
@Transactional(readOnly = true)
public class OutboxService {

    private static final int MAX_LIMIT = 100;
    private static final int MAX_BATCH_SIZE = 500;

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

    @Transactional
    public List<ClaimedOutboxEvent> claimPending(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int batchSize) {
        String normalizedWorkerId = requireWorkerId(workerId);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }

        int safeBatchSize = Math.clamp(batchSize, 1, MAX_BATCH_SIZE);
        List<UUID> claimedIds = outboxEvents.claimPending(
                normalizedWorkerId,
                now,
                now.plus(leaseDuration),
                safeBatchSize);
        if (claimedIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, OutboxEvent> eventsById = new HashMap<>();
        outboxEvents.findAllById(claimedIds)
                .forEach(event -> eventsById.put(event.getId(), event));

        return claimedIds.stream()
                .map(eventsById::get)
                .filter(Objects::nonNull)
                .map(this::toClaimedEvent)
                .toList();
    }

    @Transactional
    public void markPublished(UUID eventId, String workerId, Instant publishedAt) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(publishedAt, "publishedAt");
        requireUpdated(outboxEvents.markPublished(eventId, requireWorkerId(workerId), publishedAt), eventId);
    }

    @Transactional
    public void reschedule(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant nextRetryAt,
            String errorCode,
            String error) {
        validateFailure(eventId, retryCount, nextRetryAt, errorCode, error);
        requireUpdated(outboxEvents.reschedule(
                eventId,
                requireWorkerId(workerId),
                retryCount,
                nextRetryAt,
                normalizeErrorCode(errorCode),
                error), eventId);
    }

    @Transactional
    public void markFailed(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant failedAt,
            String errorCode,
            String error) {
        validateFailure(eventId, retryCount, failedAt, errorCode, error);
        requireUpdated(outboxEvents.markFailed(
                eventId,
                requireWorkerId(workerId),
                retryCount,
                failedAt,
                normalizeErrorCode(errorCode),
                error), eventId);
    }

    private ClaimedOutboxEvent toClaimedEvent(OutboxEvent event) {
        return new ClaimedOutboxEvent(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getRoutingKey(),
                event.getSchemaVersion(),
                event.getPayload(),
                event.getRetryCount(),
                event.getCorrelationId(),
                event.getCausationId(),
                event.getOccurredAt());
    }

    private static void validateFailure(
            UUID eventId,
            int retryCount,
            Instant timestamp,
            String errorCode,
            String error) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(error, "error");
        normalizeErrorCode(errorCode);
        if (retryCount <= 0) {
            throw new IllegalArgumentException("retryCount must be positive");
        }
    }

    private static String requireWorkerId(String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        String normalized = workerId.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("workerId must not exceed 120 characters");
        }
        return normalized;
    }

    private static String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode must not be blank");
        }
        String normalized = errorCode.trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("errorCode must not exceed 80 characters");
        }
        return normalized;
    }

    private static void requireUpdated(int updatedRows, UUID eventId) {
        if (updatedRows != 1) {
            throw new IllegalStateException("Outbox lease is no longer owned for event " + eventId);
        }
    }
}
