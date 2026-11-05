package com.company.commonstarter.http;

import lombok.experimental.UtilityClass;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

// Clone the shared, auto-configured RestClient.Builder (already carrying correlation-id and
// logging interceptors) per downstream service, adding only what that client needs on top --
// typically a JwtClientInterceptor.
@UtilityClass
public class RestClientSupport {

    public static RestClient forClient(final RestClient.Builder sharedBuilder,
                                        final String baseUrl,
                                        final ClientHttpRequestInterceptor... extraInterceptors) {
        final RestClient.Builder clone = sharedBuilder.clone().baseUrl(baseUrl);
        for (final ClientHttpRequestInterceptor interceptor : extraInterceptors) {
            clone.requestInterceptor(interceptor);
        }
        return clone.build();
    }

    public static <T> T proxyClient(final RestClient restClient, final Class<T> clientType) {
        final RestClientAdapter adapter = RestClientAdapter.create(restClient);
        final HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(clientType);
    }
}