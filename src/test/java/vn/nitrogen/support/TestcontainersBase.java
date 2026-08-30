package vn.nitrogen.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Hạ tầng thật cho integration test: PostgreSQL và RabbitMQ trong container.
 *
 * <p>Không dùng H2 — schema-per-module, JSONB, partial index và
 * {@code jsonb_typeof()} trong CHECK constraint đều là đặc thù PostgreSQL. Test
 * trên engine khác chỉ chứng minh code chạy được trên engine khác.
 *
 * <p>Container khai báo {@code static} nên dùng chung cho mọi test class kế thừa
 * — khởi động một lần cho cả build thay vì một lần cho mỗi class.
 *
 * <p>PostgreSQL 18 khớp RDS development hiện tại.
 */
@Tag("docker")
@Testcontainers
@ActiveProfiles("test")
public abstract class TestcontainersBase {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("nitrogen")
                    .withUsername("nitrogen")
                    .withPassword("nitrogen");

    @ServiceConnection
    protected static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }
}
