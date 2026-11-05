package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.config.JwtClientProperties;
import com.company.commonstarter.jwt.client.JwtClientInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtClientAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(JwtClientProperties.class));
    }

    @Test
    void bindsPropertiesWhenEnabled() {
        contextRunner.withPropertyValues("company.jwt.enabled=true")
                .run(context -> assertThat(context.getBean(JwtClientProperties.class).isEnabled()).isTrue());
    }

    @Test
    void configuresOneInterceptorPerNamedClient() {
        contextRunner.withPropertyValues("company.jwt.enabled=true",
                        "company.jwt.clients.order-service.secret=order-secret-at-least-20-characters",
                        "company.jwt.clients.billing-service.secret=billing-secret-at-least-20-characters",
                        "company.jwt.clients.billing-service.auth-scheme=X_API_AUTHORIZATION")
                .run(context -> {
                    final Map<String, JwtClientInterceptor> interceptors =
                            context.getBean("jwtClientInterceptors", Map.class);
                    assertThat(interceptors).containsOnlyKeys("order-service", "billing-service");
                    assertThat(interceptors.get("order-service").getValidToken())
                            .isNotEqualTo(interceptors.get("billing-service").getValidToken());
                });
    }

    @Test
    void returnsEmptyMapWithNoClientsConfigured() {
        contextRunner.withPropertyValues("company.jwt.enabled=true")
                .run(context -> assertThat(context.getBean("jwtClientInterceptors", Map.class)).isEmpty());
    }

    @Test
    void backsOffWhenConsumerDefinesOwnInterceptorsMap() {
        contextRunner.withUserConfiguration(CustomInterceptorsConfig.class)
                .withPropertyValues("company.jwt.enabled=true",
                        "company.jwt.clients.order-service.secret=order-secret-at-least-20-characters")
                .run(context -> assertThat(context.getBean("jwtClientInterceptors", Map.class))
                        .isSameAs(CustomInterceptorsConfig.CUSTOM_INTERCEPTORS));
    }

    @Test
    void staysDisabledWhenJjwtMissingFromClasspath() {
        contextRunner.withPropertyValues("company.jwt.enabled=true")
                .withClassLoader(new FilteredClassLoader(io.jsonwebtoken.Jwts.class))
                .run(context -> assertThat(context).doesNotHaveBean(JwtClientProperties.class));
    }

    @Configuration
    static class CustomInterceptorsConfig {

        static final Map<String, JwtClientInterceptor> CUSTOM_INTERCEPTORS = Map.of();

        @Bean
        Map<String, JwtClientInterceptor> jwtClientInterceptors() {
            return CUSTOM_INTERCEPTORS;
        }
    }
}