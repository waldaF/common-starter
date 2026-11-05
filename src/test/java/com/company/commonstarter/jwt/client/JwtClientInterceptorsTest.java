package com.company.commonstarter.jwt.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JwtClientInterceptorsTest {

    @Test
    void returnsConfiguredInterceptor() {
        final JwtClientInterceptor interceptor = new JwtClientInterceptor(
                new JwtClientSettings("order-secret-at-least-20-characters", 15, 2),
                "order-service",
                JwtAuthScheme.BEARER);

        assertThat(JwtClientInterceptors.require(Map.of("order-service", interceptor), "order-service"))
                .isSameAs(interceptor);
    }

    @Test
    void failsFastWithPropertyPathWhenClientNotConfigured() {
        assertThatIllegalStateException()
                .isThrownBy(() -> JwtClientInterceptors.require(Map.of(), "order-service"))
                .withMessageContaining("company.jwt.clients.order-service.secret");
    }
}