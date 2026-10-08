package vn.nitrogen.integration.repository;

import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedMessageRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ProcessedMessageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @return {@code true} khi caller giành quyền xử lý message; {@code false}
     * khi cùng consumer đã xử lý message này.
     */
    public boolean tryInsert(String consumerName, UUID messageId, UUID correlationId) {
        int insertedRows = jdbc.update("""
                INSERT INTO integration.processed_messages (
                    consumer_name,
                    message_id,
                    correlation_id,
                    processed_at
                )
                VALUES (
                    :consumerName,
                    :messageId,
                    :correlationId,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (consumer_name, message_id) DO NOTHING
                """, Map.of(
                "consumerName", consumerName,
                "messageId", messageId,
                "correlationId", correlationId));

        return insertedRows == 1;
    }
}
