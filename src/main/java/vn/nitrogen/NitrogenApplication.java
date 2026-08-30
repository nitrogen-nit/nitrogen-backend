package vn.nitrogen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Nitrogen — nền tảng học và luyện thi Hoá học THPT.
 *
 * <p>Một application backend duy nhất, chạy nhiều replica; không tách microservice
 * trước khi có bằng chứng tải hoặc nhu cầu release độc lập (§1, ADR-001).
 *
 * <p>Một artifact, hai chế độ chạy, ghép với profile môi trường:
 * <ul>
 *   <li>{@code --spring.profiles.active=web,local} — REST ở máy local</li>
 *   <li>{@code --spring.profiles.active=worker,dev} — worker ở development</li>
 *   <li>{@code --spring.profiles.active=web,prod} — REST ở production</li>
 * </ul>
 */
@SpringBootApplication
@Modulithic(systemName = "Nitrogen", sharedModules = { "common", "platform" })
@EnableScheduling
public class NitrogenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NitrogenApplication.class, args);
    }
}
