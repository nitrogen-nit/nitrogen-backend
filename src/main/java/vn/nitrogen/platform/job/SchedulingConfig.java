package vn.nitrogen.platform.job;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler chỉ chạy ở chế độ web.
 *
 * <p>Worker replica cố ý không có scheduler: nếu cả hai chế độ cùng chạy job
 * định kỳ thì outbox publisher và auto-submit sẽ nhân đôi số lần thực thi mà
 * không ai chủ ý.
 */
@Configuration
@Profile("web")
@EnableScheduling
public class SchedulingConfig {
}
