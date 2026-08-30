package vn.nitrogen.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import vn.nitrogen.support.TestcontainersBase;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "web"})
@Tag("docker")
@Tag("integration")
class ObservabilityEndpointIntegrationTest extends TestcontainersBase {

    @LocalServerPort
    private int port;

    @Test
    void exposesStandardHealthGroupsWithoutAuthentication() {
        assertHealthUp("/actuator/health");
        assertHealthUp("/actuator/health/liveness");
        assertHealthUp("/actuator/health/readiness");
    }

    @Test
    void exposesPrometheusMetricsWithCommonTagsWithoutAuthentication() {
        get("/actuator/health/readiness");
        Response response = get("/actuator/prometheus");

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.body()).contains("jvm_");
        assertThat(response.body()).contains("process_");
        assertThat(response.body()).contains("application=\"nitrogen-backend\"");
        assertThat(response.body()).contains("environment=\"test\"");
        assertThat(response.body()).contains("version=\"test\"");
    }

    @Test
    void echoesValidCorrelationIdInResponseHeader() {
        String correlationId = "integration-test-correlation";

        Response response = RestClient.create()
                .get()
                .uri("http://localhost:" + port + "/actuator/health/liveness")
                .header(CorrelationId.HEADER_NAME, correlationId)
                .exchange((request, clientResponse) -> new Response(
                        clientResponse.getStatusCode(),
                        clientResponse.bodyTo(String.class),
                        clientResponse.getHeaders().getFirst(CorrelationId.HEADER_NAME)));

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.correlationId()).isEqualTo(correlationId);
    }

    private void assertHealthUp(String path) {
        Response response = get(path);

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    private Response get(String path) {
        return RestClient.create()
                .get()
                .uri("http://localhost:" + port + path)
                .exchange((request, response) -> new Response(
                        response.getStatusCode(),
                        response.bodyTo(String.class),
                        response.getHeaders().getFirst(CorrelationId.HEADER_NAME)));
    }

    private record Response(HttpStatusCode status, String body, String correlationId) {
    }
}
