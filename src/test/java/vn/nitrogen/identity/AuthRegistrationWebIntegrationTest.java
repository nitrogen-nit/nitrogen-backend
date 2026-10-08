package vn.nitrogen.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.nitrogen.support.TestcontainersBase;

/**
 * Đăng ký và xác minh email qua HTTP thật, với PostgreSQL thật.
 *
 * <p>Đọc bảng của administration/integration bằng SQL thay vì repository của
 * module đó: test kiểm cái đã thực sự xuống DB, không đi qua code đang được kiểm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "web"})
@Tag("docker")
@Tag("integration")
class AuthRegistrationWebIntegrationTest extends TestcontainersBase {

    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersPendingUserThenVerifiesEmailWithoutAuthenticationOrCsrf() throws Exception {
        String email = uniqueEmail();

        register(email.toUpperCase(), PASSWORD)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("VERIFICATION_PENDING"))
                .andExpect(jsonPath("$.id").doesNotExist());

        Map<String, Object> user = jdbc.queryForMap(
                "select id, status, password_hash, email_verified from identity.users where email = ?", email);
        UUID userId = (UUID) user.get("id");
        assertThat(user.get("status")).isEqualTo("PENDING");
        assertThat(user.get("email_verified")).isEqualTo(false);
        String passwordHash = (String) user.get("password_hash");
        assertThat(passwordHash).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, passwordHash)).isTrue();

        assertThat(count("select count(*) from administration.audit_logs where target_id = ? and action = 'USER_REGISTERED'", userId))
                .isEqualTo(1);
        assertThat(count("select count(*) from administration.security_events where actor_id = ? and event_type = 'REGISTRATION_SUCCESS'", userId))
                .isEqualTo(1);

        JsonNode registered = outboxPayload(userId, "identity.UserRegistered");
        assertThat(validate("identity.user-registered.v1.json", registered)).isEmpty();

        JsonNode verification = outboxPayload(userId, "identity.EmailVerificationRequested");
        assertThat(validate("identity.email-verification-requested.v1.json", verification)).isEmpty();
        String rawToken = verification.get("verificationToken").asText();
        assertThat(count("select count(*) from identity.email_verification_tokens where token_hash = ?", rawToken))
                .as("token thô không được lưu trong bảng token")
                .isZero();

        verifyEmail(rawToken).andExpect(status().isNoContent());

        Map<String, Object> verified = jdbc.queryForMap(
                "select status, email_verified, email_verified_at from identity.users where id = ?", userId);
        assertThat(verified.get("status")).isEqualTo("ACTIVE");
        assertThat(verified.get("email_verified")).isEqualTo(true);
        assertThat(verified.get("email_verified_at")).isNotNull();
        assertThat(count("select count(*) from administration.audit_logs where target_id = ? and action = 'USER_EMAIL_VERIFIED'", userId))
                .isEqualTo(1);

        verifyEmail(rawToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_VERIFICATION_TOKEN_INVALID"));
        assertThat(count("select count(*) from administration.security_events where actor_id = ? and event_type = 'EMAIL_VERIFICATION_FAILURE'", userId))
                .isEqualTo(1);
    }

    @Test
    void duplicateEmailIsIndistinguishableFromNewRegistration() throws Exception {
        String email = uniqueEmail();
        String firstBody = register(email, PASSWORD)
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        UUID userId = jdbc.queryForObject("select id from identity.users where email = ?", UUID.class, email);

        register(email.toUpperCase(), "another password")
                .andExpect(status().isAccepted())
                .andExpect(content().json(firstBody, true));

        assertThat(count("select count(*) from identity.users where lower(email) = ?", email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select password_hash from identity.users where id = ?", String.class, userId))
                .satisfies(hash -> assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue());
        assertThat(count("select count(*) from identity.email_verification_tokens where user_id = ?", userId)).isEqualTo(1);
        assertThat(count("select count(*) from integration.outbox_events where aggregate_id = ?", userId)).isEqualTo(2);
        assertThat(count("select count(*) from administration.security_events where actor_id = ? and event_type = 'REGISTRATION_DUPLICATE_EMAIL'", userId))
                .isEqualTo(1);
    }

    @Test
    void rejectsInvalidRegistrationInput() throws Exception {
        register("not-an-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        register(uniqueEmail(), "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        register(uniqueEmail(), "ệ".repeat(30))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_POLICY_VIOLATION"));
    }

    @Test
    void unknownVerificationTokenIsRejected() throws Exception {
        verifyEmail("does-not-exist")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_VERIFICATION_TOKEN_INVALID"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password,
                        "displayName", "Learner"))));
    }

    private ResultActions verifyEmail(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
    }

    private JsonNode outboxPayload(UUID userId, String eventType) throws Exception {
        String payload = jdbc.queryForObject(
                "select payload::text from integration.outbox_events where aggregate_id = ? and event_type = ?",
                String.class, userId, eventType);
        return objectMapper.readTree(payload);
    }

    private Integer count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private Set<ValidationMessage> validate(String schemaFile, JsonNode payload) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/json-schema/messages/" + schemaFile)) {
            assertThat(in).as("json-schema/messages/%s phải có trên classpath", schemaFile).isNotNull();
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in);
            return schema.validate(payload);
        }
    }

    private static String uniqueEmail() {
        return "learner-%s@example.test".formatted(UUID.randomUUID());
    }
}
