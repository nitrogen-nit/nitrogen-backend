package vn.nitrogen.platform.runtime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@Tag("unit")
class WebStartupReporterTest {

    @Test
    void reportsStartupWithoutThrowing() {
        WebStartupReporter reporter = new WebStartupReporter(new MockEnvironment());

        assertThatNoException().isThrownBy(() -> reporter.run(null));
    }

    @Test
    void prefersActualLocalServerPort() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("local.server.port", "18080")
                .withProperty("server.port", "8080");

        assertThat(new WebStartupReporter(environment).resolvePort()).isEqualTo(18080);
    }

    @Test
    void fallsBackToConfiguredServerPort() {
        MockEnvironment environment = new MockEnvironment().withProperty("server.port", "9090");

        assertThat(new WebStartupReporter(environment).resolvePort()).isEqualTo(9090);
    }

    @Test
    void fallsBackToDefaultWebPort() {
        assertThat(new WebStartupReporter(new MockEnvironment()).resolvePort()).isEqualTo(8080);
    }
}
