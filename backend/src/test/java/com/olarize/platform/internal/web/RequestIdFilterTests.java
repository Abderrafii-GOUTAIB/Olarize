package com.olarize.platform.internal.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class RequestIdFilterTests {
    @Test void completionLogHasCorrelationButNoSensitiveRequestData(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/employees/private-value");
        request.setQueryString("token=sensitive-query-value");
        request.addHeader("Authorization", "Bearer sensitive-token-value");
        request.addHeader("X-Request-ID", "safe-request-id");
        var response = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(request, response, (req, res) ->
            ((MockHttpServletResponse) res).setStatus(403));
        assertThat(output).contains("http.request method=GET status=403 durationMs=")
            
            .doesNotContain("/api/employees/private-value", "sensitive-query-value", "sensitive-token-value");
        assertThat(response.getHeader("X-Request-ID")).isEqualTo("safe-request-id");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test void healthPollDoesNotWriteCompletionLog(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        new RequestIdFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        assertThat(output).doesNotContain("http.request method=GET");
    }

    @Test void invalidIdsAreReplacedAndContextIsClearedEvenOnFailure() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "bad id with spaces");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new RequestIdFilter().doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
            throw new ServletException("failure");
        })).isInstanceOf(ServletException.class);
        assertThat(response.getHeader("X-Request-ID")).matches("[a-f0-9-]{36}");
        assertThat(MDC.get("requestId")).isNull();
    }
}
