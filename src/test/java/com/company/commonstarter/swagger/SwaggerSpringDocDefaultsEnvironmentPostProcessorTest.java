package com.company.commonstarter.swagger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class   SwaggerSpringDocDefaultsEnvironmentPostProcessorTest {

    private final SwaggerSpringDocDefaultsEnvironmentPostProcessor postProcessor =
            new SwaggerSpringDocDefaultsEnvironmentPostProcessor();

    @Test
    void doesNothingWhenSwaggerDisabled() {
        final MockEnvironment environment = new MockEnvironment();

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().contains("commonStarterSwaggerDefaults")).isFalse();
    }

    @Test
    void seedsSpringdocDefaultsWhenSwaggerEnabled() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("company.swagger.enabled", "true");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("springdoc.api-docs.path")).isEqualTo("/api-docs");
        assertThat(environment.getProperty("springdoc.swagger-ui.path")).isEqualTo("/swagger-ui.html");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("springdoc.swagger-ui.disable-swagger-default-url", Boolean.class)).isTrue();
        assertThat(environment.getProperty("springdoc.swagger-ui.display-request-duration", Boolean.class)).isTrue();
        assertThat(environment.getProperty("springdoc.packages-to-scan")).isNull();
    }

    @Test
    void explicitSpringdocPropertyWins() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("company.swagger.enabled", "true");
        environment.setProperty("springdoc.swagger-ui.path", "/custom-ui.html");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("springdoc.swagger-ui.path")).isEqualTo("/custom-ui.html");
    }

    @Test
    void forwardsSwaggerPackagesToScan() {
        final MockEnvironment environment = new MockEnvironment();
        environment.setProperty("company.swagger.enabled", "true");
        environment.setProperty("company.swagger.packages-to-scan", "com.example.app.controller");

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("springdoc.packages-to-scan"))
                .isEqualTo("com.example.app.controller");
    }
}