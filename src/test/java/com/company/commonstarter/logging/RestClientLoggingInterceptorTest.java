package com.company.commonstarter.logging;

import com.company.commonstarter.config.LoggingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientLoggingInterceptorTest {

    private final LoggingProperties properties = new LoggingProperties();
    private final List<LoggingVo> logged = new ArrayList<>();
    private final LoggingService recordingLoggingService = new LoggingService() {
        @Override
        public void logRequestResponse(final LoggingVo loggingVo) {
            logged.add(loggingVo);
        }

        @Override
        public void clearRequestResponse() {
        }
    };

    @Test
    void capturesTimingAndKeepsResponseBodyReadableByCaller() {
        final RestClientLoggingInterceptor interceptor =
                new RestClientLoggingInterceptor(properties, recordingLoggingService, List.of());
        final RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();

        server.expect(requestTo("/users/1"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        final String body = restClient.method(HttpMethod.GET).uri("/users/1").retrieve().body(String.class);

        assertThat(body).isEqualTo("{\"id\":1}");
        assertThat(logged).hasSize(1);
        assertThat(logged.get(0).responseBody()).isEqualTo("{\"id\":1}");
        assertThat(logged.get(0).status()).isEqualTo(200);
        assertThat(logged.get(0).error()).isFalse();
    }

    @Test
    void flagsServerErrorStatusAsError() {
        final RestClientLoggingInterceptor interceptor =
                new RestClientLoggingInterceptor(properties, recordingLoggingService, List.of());
        final RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();

        server.expect(requestTo("/boom"))
                .andRespond(request -> new org.springframework.mock.http.client.MockClientHttpResponse(
                        new byte[0], org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        restClient.method(HttpMethod.GET).uri("/boom").retrieve().onStatus(s -> true, (req, res) -> { })
                .toBodilessEntity();

        assertThat(logged.get(0).error()).isTrue();
    }
}