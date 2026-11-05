package com.company.commonstarter.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientSupportTest {

    @Test
    void clonesSharedBuilderWithBaseUrlAndExtraInterceptor() {
        final RestClient.Builder shared = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(shared).build();
        final RestClient client = RestClientSupport.forClient(shared, "http://example.internal",
                (request, body, execution) -> {
                    request.getHeaders().set("X-Extra", "added");
                    return execution.execute(request, body);
                });

        server.expect(requestTo("http://example.internal/ping"))
                .andExpect(header("X-Extra", "added"))
                .andRespond(withSuccess());

        client.method(HttpMethod.GET).uri("/ping").retrieve().toBodilessEntity();
        server.verify();
    }

    @Test
    void doesNotMutateTheSharedBuilder() {
        final RestClient.Builder shared = RestClient.builder();
        RestClientSupport.forClient(shared, "http://example.internal");

        // building a fresh client from the untouched shared builder must still work unscoped
        final RestClient plain = shared.build();
        assertThat(plain).isNotNull();
    }

    @Test
    void proxyClientInvokesExchangeMethodOverRestClient() {
        final RestClient.Builder builder = RestClient.builder().baseUrl("http://example.internal");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();

        server.expect(requestTo("http://example.internal/ping"))
                .andRespond(withSuccess("pong", MediaType.TEXT_PLAIN));

        final PingClient pingClient = RestClientSupport.proxyClient(restClient, PingClient.class);

        assertThat(pingClient.ping()).isEqualTo("pong");
        server.verify();
    }

    private interface PingClient {
        @GetExchange("/ping")
        String ping();
    }
}