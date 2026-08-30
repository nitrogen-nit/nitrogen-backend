package vn.nitrogen.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Filter chain tối thiểu để app bootstrap được.
 *
 * <p>Phạm vi hiện tại: stateless API, không dùng cookie session, mở actuator
 * probe và OpenAPI, chặn phần còn lại.
 *
 * <p>TODO: chưa cấu hình JWT resource server, ma trận quyền theo role và
 * chính sách exam integrity — sẽ bổ sung ở PR security.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // API dùng bearer token/stateless, không dùng browser cookie
                // session. Giữ CSRF cho surface khác, chỉ bỏ qua API path.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                // Không có credential ⇒ 401, không phải 403. Mặc định của Spring
                // Security trả 403 cho anonymous, khiến client không phân biệt được
                // "chưa đăng nhập" với "đăng nhập rồi nhưng không đủ quyền".
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }
}
