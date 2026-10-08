package vn.nitrogen.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Filter chain tối thiểu để app bootstrap được.
 *
 * <p>Phạm vi hiện tại: stateless API, không dùng cookie session, giữ CSRF mặc
 * định của Spring Security, mở actuator probe và OpenAPI, chặn phần còn lại.
 *
 * <p>TODO: chưa cấu hình JWT resource server, ma trận quyền theo role và
 * chính sách exam integrity — sẽ bổ sung ở PR security.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Endpoint xác thực mở cho anonymous — chính chúng là cách để có credential. */
    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/registrations",
            "/api/v1/auth/email-verifications"
    };

    /**
     * Hash có tiền tố thuật toán ({@code {bcrypt}...}) để sau này đổi thuật toán
     * mà hash cũ vẫn kiểm được.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /*
     * java:S4502 — CSRF chỉ bị bỏ cho PUBLIC_AUTH_ENDPOINTS: request anonymous,
     * API stateless, không có session/cookie xác thực nào để trình duyệt tự gửi
     * kèm, nên không có gì để giả mạo. Mọi endpoint khác vẫn giữ CSRF mặc định.
     */
    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
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
                // Endpoint công khai không có session hay cookie nào để CSRF lợi dụng;
                // client chưa đăng nhập cũng không có cách lấy CSRF token.
                .csrf(csrf -> csrf.ignoringRequestMatchers(PUBLIC_AUTH_ENDPOINTS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }
}
