package vn.nitrogen.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("unit")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesCorrelationIdWhenRequestDoesNotContainHeader() throws ServletException, IOException {
        MockHttpServletResponse response = execute(new MockHttpServletRequest()).response();

        String correlationId = response.getHeader(CorrelationId.HEADER_NAME);
        assertThat(correlationId).isNotBlank();
        assertThatCodeCanParseUuid(correlationId);
    }

    @Test
    void keepsValidCorrelationIdFromRequestAndReturnsItInResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER_NAME, "nitrogen.dev-123");

        Result result = execute(request);

        assertThat(result.response().getHeader(CorrelationId.HEADER_NAME)).isEqualTo("nitrogen.dev-123");
        assertThat(result.correlationIdInChain()).isEqualTo("nitrogen.dev-123");
    }

    @Test
    void putsCorrelationIdInLogContextOnlyDuringRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER_NAME, "traceable-request");

        Result result = execute(request);

        assertThat(result.correlationIdInChain()).isEqualTo("traceable-request");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void doesNotLeakMdcBetweenRequests() throws ServletException, IOException {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.addHeader(CorrelationId.HEADER_NAME, "first-request");
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();

        Result first = execute(firstRequest);
        Result second = execute(secondRequest);

        assertThat(first.correlationIdInChain()).isEqualTo("first-request");
        assertThat(second.correlationIdInChain()).isNotEqualTo("first-request");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void rejectsHeaderWithNewlineOrDangerousCharacters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER_NAME, "danger\nid");

        Result result = execute(request);

        assertThat(result.response().getHeader(CorrelationId.HEADER_NAME)).isNotEqualTo("danger\nid");
        assertThatCodeCanParseUuid(result.response().getHeader(CorrelationId.HEADER_NAME));
    }

    @Test
    void rejectsBlankTooLongAndUnsafeCorrelationIdValues() {
        assertThat(CorrelationId.normalize("")).isEmpty();
        assertThat(CorrelationId.normalize(" ".repeat(129))).isEmpty();
        assertThat(CorrelationId.normalize("-starts-with-symbol")).isEmpty();
        assertThat(CorrelationId.normalize("safe.value:123")).contains("safe.value:123");
    }

    @Test
    void createsStringCorrelationIdWhenMdcIsEmpty() {
        assertThat(CorrelationId.currentOrNew()).isNotBlank();
        assertThatCodeCanParseUuid(CorrelationId.currentUuidOrNew().toString());
    }

    private Result execute(MockHttpServletRequest request) throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationId.MDC_KEY));

        filter.doFilter(request, response, chain);

        return new Result(response, correlationIdInChain.get());
    }

    private static void assertThatCodeCanParseUuid(String value) {
        UUID.fromString(value);
    }

    private record Result(MockHttpServletResponse response, String correlationIdInChain) {
    }
}
