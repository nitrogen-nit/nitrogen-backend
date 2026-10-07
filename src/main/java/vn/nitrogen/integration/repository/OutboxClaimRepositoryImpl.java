package vn.nitrogen.integration.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxClaimRepositoryImpl implements OutboxClaimRepository {

    private static final String CLAIM_SQL = """
            WITH candidates AS (
                SELECT id
                FROM integration.outbox_events
                WHERE status = 'PENDING'
                  AND next_retry_at <= :now
                  AND (locked_until IS NULL OR locked_until < :now)
                ORDER BY occurred_at, id
                FOR UPDATE SKIP LOCKED
                LIMIT :batchSize
            )
            UPDATE integration.outbox_events AS event
            SET locked_by = :workerId,
                locked_until = :lockedUntil
            FROM candidates
            WHERE event.id = candidates.id
            RETURNING event.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public OutboxClaimRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UUID> claimPending(
            String workerId,
            Instant now,
            Instant lockedUntil,
            int batchSize) {
        Map<String, Object> parameters = Map.of(
                "workerId", workerId,
                "now", Timestamp.from(now),
                "lockedUntil", Timestamp.from(lockedUntil),
                "batchSize", batchSize);

        return jdbc.query(
                CLAIM_SQL,
                parameters,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class));
    }

    @Override
    public int markPublished(UUID eventId, String workerId, Instant publishedAt) {
        return jdbc.update("""
                UPDATE integration.outbox_events
                SET status = 'PUBLISHED',
                    published_at = :publishedAt,
                    locked_by = NULL,
                    locked_until = NULL,
                    last_error_code = NULL,
                    last_error = NULL
                WHERE id = :eventId
                  AND status = 'PENDING'
                  AND locked_by = :workerId
                """, Map.of(
                "eventId", eventId,
                "workerId", workerId,
                "publishedAt", Timestamp.from(publishedAt)));
    }

    @Override
    public int reschedule(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant nextRetryAt,
            String errorCode,
            String error) {
        return jdbc.update("""
                UPDATE integration.outbox_events
                SET retry_count = :retryCount,
                    next_retry_at = :nextRetryAt,
                    locked_by = NULL,
                    locked_until = NULL,
                    last_error_code = :errorCode,
                    last_error = :error
                WHERE id = :eventId
                  AND status = 'PENDING'
                  AND locked_by = :workerId
                """, Map.of(
                "eventId", eventId,
                "workerId", workerId,
                "retryCount", retryCount,
                "nextRetryAt", Timestamp.from(nextRetryAt),
                "errorCode", errorCode,
                "error", error));
    }

    @Override
    public int markFailed(
            UUID eventId,
            String workerId,
            int retryCount,
            Instant failedAt,
            String errorCode,
            String error) {
        return jdbc.update("""
                UPDATE integration.outbox_events
                SET status = 'FAILED',
                    retry_count = :retryCount,
                    failed_at = :failedAt,
                    locked_by = NULL,
                    locked_until = NULL,
                    last_error_code = :errorCode,
                    last_error = :error
                WHERE id = :eventId
                  AND status = 'PENDING'
                  AND locked_by = :workerId
                """, Map.of(
                "eventId", eventId,
                "workerId", workerId,
                "retryCount", retryCount,
                "failedAt", Timestamp.from(failedAt),
                "errorCode", errorCode,
                "error", error));
    }
}
