package vn.nitrogen.platform.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("web")
final class WebStartupReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebStartupReporter.class);

    private final Environment environment;

    WebStartupReporter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        WebEndpoint endpoint = WebEndpoint.from(environment, resolvePort());
        log.info("Nitrogen backend is running on port {} at {}", endpoint.port(), endpoint.uri());
    }

    int resolvePort() {
        Integer actualPort = environment.getProperty("local.server.port", Integer.class);
        if (actualPort != null) {
            return actualPort;
        }

        return environment.getProperty("server.port", Integer.class, 8080);
    }
}
