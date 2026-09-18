package vn.nitrogen.practice;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.nitrogen.identity.domain.User;
import vn.nitrogen.identity.repository.UserRepository;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "web"})
@Tag("docker")
@Tag("integration")
class PracticeAttemptWebIntegrationTest extends TestcontainersBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository users;

    @Test
    void startsAndReadsPracticeAttempt() throws Exception {
        User learner = users.saveAndFlush(User.active("learner-%s@example.test".formatted(UUID.randomUUID()), "Learner"));
        UUID originId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/api/v1/practice-attempts")
                        .with(user("learner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", learner.getId(),
                                "attemptKind", "TOPIC_PRACTICE",
                                "originType", "CURRICULUM_NODE",
                                "originId", originId))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/practice-attempts/")))
                .andExpect(jsonPath("$.userId").value(learner.getId().toString()))
                .andExpect(jsonPath("$.attemptKind").value("TOPIC_PRACTICE"))
                .andExpect(jsonPath("$.originType").value("CURRICULUM_NODE"))
                .andExpect(jsonPath("$.originId").value(originId.toString()))
                .andExpect(jsonPath("$.attemptNo").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.gradingStatus").value("NOT_REQUIRED"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String attemptId = body.get("id").asText();

        mockMvc.perform(get("/api/v1/practice-attempts/{attemptId}", attemptId)
                        .with(user("learner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attemptId))
                .andExpect(jsonPath("$.userId").value(learner.getId().toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void rejectsSecondActiveAttemptForSameOrigin() throws Exception {
        User learner = users.saveAndFlush(User.active("learner-%s@example.test".formatted(UUID.randomUUID()), "Learner"));
        UUID originId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "userId", learner.getId(),
                "attemptKind", "TOPIC_PRACTICE",
                "originType", "CURRICULUM_NODE",
                "originId", originId));

        mockMvc.perform(post("/api/v1/practice-attempts")
                        .with(user("learner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/practice-attempts")
                        .with(user("learner"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ATTEMPT_ACTIVE_EXISTS"));
    }
}
