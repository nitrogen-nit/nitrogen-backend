package vn.nitrogen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import vn.nitrogen.support.TestcontainersBase;

/**
 * Chế độ {@code web} khởi động được và báo readiness UP.
 *
 * <p>Kiểm hai thứ mà các test khác không chạm tới: context lên được với đủ
 * profile {@code web} + {@code core}, và filter chain cho probe đi qua mà không
 * cần xác thực — nếu probe bị chặn thì orchestrator sẽ giết pod ngay khi khởi động.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "web"})
class NitrogenApplicationTests extends TestcontainersBase {

    @LocalServerPort
    private int port;

    @Test
    void readinessProbeIsUpWithoutAuthentication() {
        Response response = get("/actuator/health/readiness");

        assertThat(response.status().value()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void businessEndpointsRequireAuthentication() {
        assertThat(get("/api/anything").status().value()).isEqualTo(401);
    }

    private Response get(String path) {
        return RestClient.create()
                .get()
                .uri("http://localhost:" + port + path)
                .exchange((request, response) ->
                        new Response(response.getStatusCode(), response.bodyTo(String.class)));
    }

    private record Response(HttpStatusCode status, String body) {
    }
}
