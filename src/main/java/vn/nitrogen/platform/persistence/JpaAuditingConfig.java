package vn.nitrogen.platform.persistence;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Bật audit của Spring Data JPA cho {@code AbstractAuditingEntity}.
 *
 * <p>Khi không có authentication (job nền, migration, worker), auditor là
 * {@code system} — cố tình không để null để cột audit luôn đọc được.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM_AUDITOR);
            }
            return Optional.of(authentication.getName());
        };
    }
}
