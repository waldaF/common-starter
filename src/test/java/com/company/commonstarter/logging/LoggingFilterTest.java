package com.company.commonstarter.logging;

import com.company.commonstarter.config.LoggingProperties;
import com.company.commonstarter.masking.TextUpdater;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingFilterTest {

    private final LoggingProperties properties = new LoggingProperties();
    private final RecordingLoggingService loggingService = new RecordingLoggingService();

    @Test
    void capturesRequestAndResponseBodiesAndDelegatesToLoggingService() throws Exception {
        final LoggingFilter filter = new LoggingFilter(properties, loggingService, List.of());
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        request.setContent("{\"item\":\"x\"}".getBytes(StandardCharsets.UTF_8));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> {
            req.getInputStream().readAllBytes(); // simulates Spring MVC reading @RequestBody
            ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201);
            res.getOutputStream().write("{\"id\":1}".getBytes(StandardCharsets.UTF_8));
        };

        filter.doFilter(request, response, chain);

        assertThat(loggingService.logged).hasSize(1);
        final LoggingVo vo = loggingService.logged.get(0);
        assertThat(vo.method()).isEqualTo("POST");
        assertThat(vo.requestURI()).isEqualTo("/orders");
        assertThat(vo.status()).isEqualTo(201);
        assertThat(vo.requestBody()).isEqualTo("{\"item\":\"x\"}");
        assertThat(vo.responseBody()).isEqualTo("{\"id\":1}");
    }

    @Test
    void skipsExcludedPaths() throws Exception {
        final LoggingFilter filter = new LoggingFilter(properties, loggingService, List.of());
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(loggingService.logged).isEmpty();
    }

    @Test
    void appliesTextUpdatersToBodiesAndHeaders() throws Exception {
        final TextUpdater redactor = text -> text.replace("secret", "***");
        final LoggingFilter filter = new LoggingFilter(properties, loggingService, List.of(redactor));
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setContent("{\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> req.getInputStream().readAllBytes();

        filter.doFilter(request, response, chain);

        assertThat(loggingService.logged.get(0).requestBody()).doesNotContain("secret");
    }

    @Test
    void truncatesResponseBodyBeyondConfiguredCap() throws Exception {
        // the request cache itself caps at maxBodyChars (see ContentCachingRequestWrapper), so
        // truncation is only directly observable on the response body here
        properties.setMaxBodyChars(5);
        final LoggingFilter filter = new LoggingFilter(properties, loggingService, List.of());
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> res.getOutputStream().write("0123456789".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, response, chain);

        assertThat(loggingService.logged.get(0).responseBody()).isEqualTo("01234...(truncated)");
    }

    private static final class RecordingLoggingService implements LoggingService {
        private final List<LoggingVo> logged = new java.util.ArrayList<>();

        @Override
        public void logRequestResponse(final LoggingVo loggingVo) {
            logged.add(loggingVo);
        }

        @Override
        public void clearRequestResponse() {
        }
    }
}