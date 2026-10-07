package vn.nitrogen.integration.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxClaimRepository {

    List<UUID> claimPending(
            String workerId,
            Instant now,
            Instant lockedUntil,
            int batchSize);

    int markPublished(UUID eventId, String workerId, Instant publishedAt);

    int reschedule(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant nextRetryAt,
            String errorCode,
            String error);

    int markFailed(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant failedAt,
            String errorCode,
            String error);
}
