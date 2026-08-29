package vn.nitrogen.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gắn tag chung cho toàn bộ metric để phân biệt web replica và worker replica
 * khi cùng một artifact chạy hai chế độ.
 *
 * <p>TODO: bổ sung tracing sampling, log correlation với correlationId của
 * MessageEnvelope.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name:nitrogen-backend}") String applicationName) {
        return registry -> registry.config()
                .meterFilter(MeterFilter.commonTags(
                        io.micrometer.core.instrument.Tags.of("application", applicationName)));
    }
}
