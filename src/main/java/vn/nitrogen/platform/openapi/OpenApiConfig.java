package vn.nitrogen.platform.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata cho spec springdoc sinh ra tại {@code /v3/api-docs}.
 *
 * <p>Tồn tại để spec sinh ra khớp header của
 * {@code contracts/openapi/nitrogen-api.yaml}. Không có bean này springdoc dùng
 * mặc định "OpenAPI definition / v0", và bước contract-check trong CI sẽ báo
 * lệch ngay ở dòng đầu — một cảnh báo vô nghĩa che mất những lệch thật.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nitrogenOpenApi(
            @Value("${nitrogen.api.version:0.0.1-SNAPSHOT}") String apiVersion) {
        return new OpenAPI().info(new Info()
                .title("Nitrogen API")
                .version(apiVersion)
                .description("Nền tảng học và luyện thi Hoá học THPT."));
    }
}
