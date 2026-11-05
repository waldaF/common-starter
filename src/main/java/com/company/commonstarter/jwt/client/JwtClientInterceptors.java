package com.company.commonstarter.jwt.client;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;

// auto-configured interceptor map silently returns null for an unconfigured or misspelled
@UtilityClass
public class JwtClientInterceptors {

    public static JwtClientInterceptor require(final Map<String, JwtClientInterceptor> interceptors,
                                               final String clientName) {
        final JwtClientInterceptor interceptor = interceptors.get(clientName);
        if (Objects.isNull(interceptor)) {
            throw new IllegalStateException(
                    "No JWT client configured for '" + clientName + "'. Set company.jwt.clients."
                            + clientName + ".secret (company.jwt.enabled must also be true).");
        }
        return interceptor;
    }
}