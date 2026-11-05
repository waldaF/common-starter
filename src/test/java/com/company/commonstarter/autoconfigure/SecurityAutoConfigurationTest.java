package com.company.commonstarter.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import com.company.commonstarter.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
                    org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class,
                    org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
                    AuthenticationErrorHandlingAutoConfiguration.class,
                    SecurityAutoConfiguration.class));

    // Boot's own ServletWebSecurityAutoConfiguration always supplies a fallback SecurityFilterChain
    @Test
    void offByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ApiKeyAuthFilter.class));
    }

    @Test
    void registersFilterChainWhenEnabled() {
        contextRunner.withPropertyValues(
                        "company.security.enabled=true",
                        "company.security.credentials[0]=secret")
                .run(context -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
    }

    @Test
    void backsOffWhenConsumerDefinesOwnFilterChain() {
        contextRunner.withPropertyValues("company.security.enabled=true", "company.security.credentials[0]=secret")
                .withUserConfiguration(CustomChainConfig.class)
                .run(context -> assertThat(context.getBean(SecurityFilterChain.class))
                        .isSameAs(CustomChainConfig.CUSTOM_CHAIN));
    }

    @Configuration
    static class CustomChainConfig {

        static final SecurityFilterChain CUSTOM_CHAIN = mock(SecurityFilterChain.class);

        @Bean
        SecurityFilterChain securityFilterChain() {
            return CUSTOM_CHAIN;
        }
    }
}