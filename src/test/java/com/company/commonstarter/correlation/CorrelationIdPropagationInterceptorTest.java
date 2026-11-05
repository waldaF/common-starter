package com.company.commonstarter.correlation;

import com.company.commonstarter.mdc.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CorrelationIdPropagationInterceptorTest {

    private final CorrelationIdProperties properties = new CorrelationIdProperties();
    private final CorrelationIdPropagationInterceptor interceptor = new CorrelationIdPropagationInterceptor(properties);

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void setsHeaderFromMdcOnOutgoingRequest() {
        MDC.put(MdcKeys.CORRELATION_ID, "my-correlation-id");

        final RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();

        server.expect(requestTo("/ping"))
                .andExpect(header("X-Correlation-Id", "my-correlation-id"))
                .andRespond(withSuccess());

        restClient.method(HttpMethod.GET).uri("/ping").retrieve().toBodilessEntity();
        server.verify();
    }

    @Test
    void doesNotSetHeaderWhenNoCorrelationIdPresent() {
        final RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();

        server.expect(requestTo("/ping")).andRespond(withSuccess());

        restClient.method(HttpMethod.GET).uri("/ping").retrieve().toBodilessEntity();
        server.verify();
    }
}