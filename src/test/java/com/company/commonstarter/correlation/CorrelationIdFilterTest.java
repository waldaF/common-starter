package com.company.commonstarter.correlation;

import com.company.commonstarter.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdProperties properties = new CorrelationIdProperties();
    private final CorrelationIdFilter filter = new CorrelationIdFilter(properties);

    @Test
    void generatesCorrelationIdWhenHeaderMissing() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
        assertThat(UuidValidator.isValidUuid(response.getHeader("X-Correlation-Id"))).isTrue();
    }

    @Test
    void reusesValidIncomingCorrelationId() throws Exception {
        final String incoming = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", incoming);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(incoming);
    }

    @Test
    void replacesInvalidIncomingCorrelationId() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "not-a-uuid");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isNotEqualTo("not-a-uuid");
        assertThat(UuidValidator.isValidUuid(response.getHeader("X-Correlation-Id"))).isTrue();
    }

    @Test
    void mdcAndScopedValueArePopulatedDuringChainAndClearedAfter() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicReference<String> seenDuringChain = new AtomicReference<>();
        final FilterChain chain = (req, res) -> seenDuringChain.set(MDC.get(MdcKeys.CORRELATION_ID));

        filter.doFilter(request, response, chain);

        assertThat(seenDuringChain.get()).isNotBlank();
        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
        assertThat(CorrelationIdContext.getCorrelationId()).isNull();
    }
}