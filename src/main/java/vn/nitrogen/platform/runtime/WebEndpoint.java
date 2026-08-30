package vn.nitrogen.platform.runtime;

import java.net.URI;
import org.springframework.core.env.Environment;

final class WebEndpoint {

    private final int port;
    private final URI uri;

    private WebEndpoint(int port, URI uri) {
        this.port = port;
        this.uri = uri;
    }

    static WebEndpoint from(Environment environment, int port) {
        String scheme = environment.getProperty("nitrogen.web.scheme", "http");
        String host = environment.getProperty("nitrogen.web.host", "localhost");
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        return new WebEndpoint(port, URI.create(scheme + "://" + host + ":" + port + normalize(contextPath)));
    }

    private static String normalize(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }

        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    int port() {
        return port;
    }

    URI uri() {
        return uri;
    }
}
