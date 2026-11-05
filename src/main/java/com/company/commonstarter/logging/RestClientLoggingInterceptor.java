package com.company.commonstarter.logging;

import com.company.commonstarter.config.LoggingProperties;
import com.company.commonstarter.masking.TextUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final LoggingProperties properties;
    private final LoggingService loggingService;
    private final List<TextUpdater> textUpdaters;

    @Override
    public ClientHttpResponse intercept(final HttpRequest request,
                                         final byte[] body,
                                         final ClientHttpRequestExecution execution) throws IOException {
        final long startTime = System.currentTimeMillis();
        final String requestBody = LoggingTextSupport.limitAndSanitize(
                body, properties.getMaxOutboundBodyChars(), textUpdaters);
        final String requestHeaders = LoggingTextSupport.sanitize(flattenHeaders(request), textUpdaters);

        try {
            final ClientHttpResponse response = execution.execute(request, body);
            final byte[] responseBodyBytes = StreamUtils.copyToByteArray(response.getBody());
            final long duration = System.currentTimeMillis() - startTime;
            final int status = response.getStatusCode().value();
            final boolean isError = status >= HttpStatus.INTERNAL_SERVER_ERROR.value()
                    || properties.getErrorStatusCodes().contains(status);

            final LoggingVo loggingVo = LoggingVo.builder()
                    .status(status)
                    .requestHeaders(requestHeaders)
                    .requestURI(request.getURI().toString())
                    .requestBody(requestBody)
                    .responseBody(LoggingTextSupport.limitAndSanitize(
                            responseBodyBytes, properties.getMaxOutboundBodyChars(), textUpdaters))
                    .method(request.getMethod().name())
                    .duration(duration)
                    .error(isError)
                    .build();
            loggingService.logRequestResponse(loggingVo);

            // the response body stream was already consumed above to log it; re-wrap so callers can still read it
            return new BufferingClientHttpResponseWrapper(response, responseBodyBytes);
        } catch (IOException e) {
            final long duration = System.currentTimeMillis() - startTime;
            log.error("RestClient request failed: method={}, uri={}, duration={}ms",
                    request.getMethod(), request.getURI(), duration, e);
            throw e;
        }
    }

    private String flattenHeaders(final HttpRequest request) {
        final StringBuilder headers = new StringBuilder();
        request.getHeaders().forEach((name, values) ->
                headers.append(name).append(": ").append(String.join(", ", values)).append("; "));
        return headers.toString();
    }
}