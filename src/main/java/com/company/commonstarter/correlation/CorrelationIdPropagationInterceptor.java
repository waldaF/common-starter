package com.company.commonstarter.correlation;

import com.company.commonstarter.mdc.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class CorrelationIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    private final CorrelationIdProperties properties;

    @Override
    public ClientHttpResponse intercept(final HttpRequest request,
                                         final byte[] body,
                                         final ClientHttpRequestExecution execution) throws IOException {
        final String correlationId = MdcUtils.getCorrelationId(properties.getMdcKey());

        if (StringUtils.hasText(correlationId)) {
            request.getHeaders().set(properties.getHeaderName(), correlationId);
            log.debug("Propagated correlation ID to outgoing request: {}", correlationId);
        } else {
            log.warn("No correlation ID found in request context for outgoing request to: {}", request.getURI());
        }

        return execution.execute(request, body);
    }
}