package vn.nitrogen.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.nitrogen.support.TestcontainersBase;

/**
 * Flyway chạy sạch từ một database rỗng (§16.2.5).
 *
 * <p>Đây là bài test rẻ nhất bắt được lỗi tốn kém nhất: migration chỉ chạy đúng
 * trên máy đã có schema cũ. Container luôn khởi động từ DB rỗng nên đường
 * "cài mới" được kiểm mỗi lần build.
 *
 * <p>Test cũng gián tiếp xác nhận {@code ddl-auto=validate} khớp: context không
 * lên được nếu Hibernate thấy mapping lệch với schema Flyway vừa tạo.
 */
@SpringBootTest
@Tag("docker")
@Tag("migration")
class MigrateFromEmptyDbTest extends TestcontainersBase {

    private static final List<String> MODULE_SCHEMAS = List.of(
            "identity", "curriculum", "chemistry", "content",
            "assessment", "examination", "practice", "progress",
            "flashcard", "simulation", "integration", "administration");

    @Autowired
    private DataSource dataSource;

    @Test
    void createsEverySchemaOwnedByAModule() throws Exception {
        assertThat(schemaNames()).containsAll(MODULE_SCHEMAS);
    }

    @Test
    void createsPracticeRuntimeTables() throws Exception {
        assertThat(tableNames("practice")).contains(
                "practice_attempts", "attempt_items", "exercise_responses",
                "grading_runs", "attempt_topic_results");
    }

    @Test
    void createsIntegrationOutboxTables() throws Exception {
        assertThat(tableNames("integration")).contains("outbox_events", "processed_messages");
    }

    @Test
    void appliesFlywayMigrationsOnceAndInOrder() throws Exception {
        List<String> versions = query("""
                SELECT version
                FROM flyway_history.flyway_schema_history
                WHERE version IS NOT NULL
                ORDER BY installed_rank
                """);

        assertThat(versions)
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void hasNoFailedFlywayMigration() throws Exception {
        assertThat(query("""
                SELECT script
                FROM flyway_history.flyway_schema_history
                WHERE success = false
                """)).isEmpty();
    }

    @Test
    void createsCompositeForeignKeyFromTopicResultToGradingRun() throws Exception {
        // FK (grading_run_id, attempt_id) → grading_runs(id, attempt_id) là thứ
        // duy nhất chặn được một topic result trỏ vào grading run của attempt khác.
        assertThat(query("""
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'practice.attempt_topic_results'::regclass
                  AND contype = 'f'
                  AND array_length(conkey, 1) = 2
                """)).contains("fk_topic_result_run");
    }

    @Test
    void createsPartialUniqueIndexForActiveAttempt() throws Exception {
        // Partial unique index là cách duy nhất diễn đạt "mỗi user chỉ một
        // attempt đang mở cho một origin" ở tầng database.
        assertThat(query("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'practice'
                  AND tablename = 'practice_attempts'
                  AND indexdef LIKE '%WHERE%'
                """)).contains(
                "uk_attempt_active_origin",
                "uk_attempt_resume_key_hash",
                "uk_attempt_idempotency");
    }

    @Test
    void createsIdentityUserUniquenessAndChecks() throws Exception {
        assertThat(query("""
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'identity.users'::regclass
                """)).contains("users_pkey", "chk_user_status", "chk_user_version");

        assertThat(query("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'identity'
                  AND tablename = 'users'
                """)).contains("uk_users_email");
    }

    private List<String> schemaNames() throws Exception {
        return query("SELECT schema_name FROM information_schema.schemata");
    }

    private List<String> tableNames(String schema) throws Exception {
        return query("SELECT table_name FROM information_schema.tables WHERE table_schema = '"
                + schema + "'");
    }

    private List<String> query(String sql) throws Exception {
        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        return values;
    }
}
