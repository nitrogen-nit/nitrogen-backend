package vn.nitrogen.support;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.core.env.Environment;

@Tag("unit")
class EnvironmentProfileConfigurationTest {

    private static final List<String> SECRET_MARKERS = List.of(
            "nitrogen2026",
            "nitrogen-postgres.cnw2u8268ojz",
            "AKIA",
            "-----BEGIN PRIVATE KEY-----",
            "SONAR_TOKEN=",
            "change-me");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void localProfileLoadsSafeLocalDefaults() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=web,local",
                        "NITROGEN_ENVIRONMENT=local",
                        "NITROGEN_DB_URL=jdbc:postgresql://localhost:5432/nitrogen",
                        "NITROGEN_DB_USER=nitrogen",
                        "NITROGEN_DB_PASSWORD=nitrogen",
                        "NITROGEN_RABBIT_HOST=localhost",
                        "NITROGEN_RABBIT_PORT=5672",
                        "NITROGEN_RABBIT_USER=nitrogen",
                        "NITROGEN_RABBIT_PASSWORD=nitrogen")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    Environment environment = context.getEnvironment();
                    DataSourceProperties datasource = bind(environment, "spring.datasource", DataSourceProperties.class);
                    RabbitProperties rabbit = bind(environment, "spring.rabbitmq", RabbitProperties.class);

                    assertThat(environment.getProperty("application.environment")).isEqualTo("local");
                    assertThat(datasource.getUrl()).isEqualTo("jdbc:postgresql://localhost:5432/nitrogen");
                    assertThat(datasource.getUsername()).isEqualTo("nitrogen");
                    assertThat(datasource.getPassword()).isEqualTo("nitrogen");
                    assertThat(rabbit.getHost()).isEqualTo("localhost");
                    assertThat(rabbit.getPort()).isEqualTo(5672);
                    assertThat(rabbit.getUsername()).isEqualTo("nitrogen");
                    assertThat(rabbit.getPassword()).isEqualTo("nitrogen");
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                });
    }

    @Test
    void devProfileBindsDatabaseAndRabbitFromEnvironmentContract() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=worker,dev",
                        "NITROGEN_ENVIRONMENT=dev",
                        "NITROGEN_DB_URL=jdbc:postgresql://dev-db.example:5432/nitrogen?sslmode=require",
                        "NITROGEN_DB_USER=nitrogen_app",
                        "NITROGEN_DB_PASSWORD=example-db-password",
                        "NITROGEN_DB_POOL_SIZE=7",
                        "NITROGEN_RABBIT_HOST=dev-rabbit.example",
                        "NITROGEN_RABBIT_PORT=5671",
                        "NITROGEN_RABBIT_USER=nitrogen_app",
                        "NITROGEN_RABBIT_PASSWORD=example-rabbit-password",
                        "NITROGEN_RABBIT_HEALTH_ENABLED=true",
                        "NITROGEN_LOG_LEVEL=DEBUG")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    Environment environment = context.getEnvironment();
                    DataSourceProperties datasource = bind(environment, "spring.datasource", DataSourceProperties.class);
                    RabbitProperties rabbit = bind(environment, "spring.rabbitmq", RabbitProperties.class);

                    assertThat(environment.getProperty("application.environment")).isEqualTo("dev");
                    assertThat(datasource.getUrl())
                            .isEqualTo("jdbc:postgresql://dev-db.example:5432/nitrogen?sslmode=require");
                    assertThat(datasource.getUsername()).isEqualTo("nitrogen_app");
                    assertThat(datasource.getPassword()).isEqualTo("example-db-password");
                    assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class))
                            .isEqualTo(7);
                    assertThat(rabbit.getHost()).isEqualTo("dev-rabbit.example");
                    assertThat(rabbit.getPort()).isEqualTo(5671);
                    assertThat(rabbit.getUsername()).isEqualTo("nitrogen_app");
                    assertThat(rabbit.getPassword()).isEqualTo("example-rabbit-password");
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
                });
    }

    @Test
    void prodProfileDisablesApplicationFlyway() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=web,prod",
                        "NITROGEN_ENVIRONMENT=prod",
                        "NITROGEN_DB_URL=jdbc:postgresql://prod-db.example:5432/nitrogen?sslmode=require",
                        "NITROGEN_DB_USER=nitrogen_app",
                        "NITROGEN_DB_PASSWORD=example-db-password",
                        "NITROGEN_RABBIT_HOST=prod-rabbit.example",
                        "NITROGEN_RABBIT_PORT=5672",
                        "NITROGEN_RABBIT_USER=nitrogen_app",
                        "NITROGEN_RABBIT_PASSWORD=example-rabbit-password")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    Environment environment = context.getEnvironment();
                    assertThat(environment.getProperty("application.environment")).isEqualTo("prod");
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
                    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                });
    }

    @Test
    void sharedEnvironmentProfilesDoNotDeclareLocalDatabaseDefaults() throws IOException {
        String sharedConfig = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String devConfig = Files.readString(Path.of("src/main/resources/application-dev.yml"), StandardCharsets.UTF_8);
        String prodConfig = Files.readString(Path.of("src/main/resources/application-prod.yml"), StandardCharsets.UTF_8);

        assertThat(sharedConfig).contains("url: ${NITROGEN_DB_URL}");
        assertThat(devConfig).doesNotContain("jdbc:postgresql://localhost");
        assertThat(prodConfig).doesNotContain("jdbc:postgresql://localhost");
        assertThat(sharedConfig + devConfig + prodConfig).doesNotContain("password: nitrogen");
    }

    @Test
    void configurationResourcesDoNotContainRealSecrets() throws IOException {
        for (Path configFile : configurationFiles()) {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);

            for (String marker : SECRET_MARKERS) {
                assertThat(content)
                        .as("Configuration file %s must not contain %s", configFile, marker)
                        .doesNotContain(marker);
            }
        }
    }

    private static <T> T bind(Environment environment, String prefix, Class<T> targetType) {
        return Binder.get(environment)
                .bind(prefix, targetType)
                .orElseThrow(() -> new AssertionError("Missing binding for " + prefix));
    }

    private static List<Path> configurationFiles() throws IOException {
        try (Stream<Path> resourceFiles = Files.walk(Path.of("src/main/resources"))) {
            List<Path> files = resourceFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.endsWith(".yml")
                                || filename.endsWith(".yaml")
                                || filename.endsWith(".properties")
                                || filename.endsWith(".txt");
                    })
                    .toList();

            return Stream.concat(files.stream(), Stream.of(Path.of(".env.local.example"))).toList();
        }
    }
}
