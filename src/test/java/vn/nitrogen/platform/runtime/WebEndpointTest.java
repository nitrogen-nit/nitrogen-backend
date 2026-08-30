package vn.nitrogen.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@Tag("unit")
class WebEndpointTest {

    @Test
    void buildsDefaultLocalEndpoint() {
        WebEndpoint endpoint = WebEndpoint.from(new MockEnvironment(), 8080);

        assertThat(endpoint.port()).isEqualTo(8080);
        assertThat(endpoint.uri()).hasToString("http://localhost:8080");
    }

    @Test
    void buildsEndpointWithCustomHostSchemeAndContextPath() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("nitrogen.web.scheme", "https")
                .withProperty("nitrogen.web.host", "api.nitrogen.local")
                .withProperty("server.servlet.context-path", "backend");

        WebEndpoint endpoint = WebEndpoint.from(environment, 8443);

        assertThat(endpoint.uri()).hasToString("https://api.nitrogen.local:8443/backend");
    }

    @Test
    void preservesLeadingSlashInContextPath() {
        MockEnvironment environment = new MockEnvironment().withProperty("server.servlet.context-path", "/api");

        WebEndpoint endpoint = WebEndpoint.from(environment, 8080);

        assertThat(endpoint.uri()).hasToString("http://localhost:8080/api");
    }
}
