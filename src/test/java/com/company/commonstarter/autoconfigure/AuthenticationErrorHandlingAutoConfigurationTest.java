package com.company.commonstarter.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withConfiguration(AutoConfigurations.of(AuthenticationErrorHandlingAutoConfiguration.class));

    @Test
    void registersJsonAuthenticationEntryPointByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(AuthenticationEntryPoint.class));
    }

    @Test
    void disabledByProperty() {
        contextRunner.withPropertyValues("company.error-handling.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AuthenticationEntryPoint.class));
    }

    @Test
    void staysOffWhenSpringSecurityMissingFromClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(AuthenticationEntryPoint.class))
                .run(context -> assertThat(context).doesNotHaveBean(AuthenticationEntryPoint.class));
    }

    @Test
    void backsOffWhenConsumerDefinesOwnEntryPoint() {
        contextRunner.withUserConfiguration(CustomEntryPointConfig.class)
                .run(context -> assertThat(context.getBean(AuthenticationEntryPoint.class))
                        .isSameAs(CustomEntryPointConfig.CUSTOM_ENTRY_POINT));
    }

    @Configuration
    static class CustomEntryPointConfig {

        static final AuthenticationEntryPoint CUSTOM_ENTRY_POINT = (request, response, authException) -> { };

        @Bean
        AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
            return CUSTOM_ENTRY_POINT;
        }
    }
}