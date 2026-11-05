package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.logging.LoggingVo;
import com.company.commonstarter.logging.ConsoleJsonMdcLoggingService;
import com.company.commonstarter.logging.ConsoleLoggingService;
import com.company.commonstarter.logging.LoggingFilter;
import com.company.commonstarter.logging.LoggingService;
import com.company.commonstarter.logging.RestClientLoggingInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class));

    @Test
    void defaultsToJsonMdcLoggingService() {
        contextRunner.run(context -> assertThat(context.getBean(LoggingService.class))
                .isInstanceOf(ConsoleJsonMdcLoggingService.class));
    }

    @Test
    void switchesToSimpleConsoleLoggingWhenConfigured() {
        contextRunner.withPropertyValues("company.logging.simple-console-logging=true")
                .run(context -> assertThat(context.getBean(LoggingService.class))
                        .isInstanceOf(ConsoleLoggingService.class));
    }

    @Test
    void consumerSuppliedLoggingServiceWins() {
        contextRunner.withUserConfiguration(CustomLoggingServiceConfig.class)
                .run(context -> assertThat(context.getBean(LoggingService.class))
                        .isInstanceOf(CustomLoggingService.class));
    }

    @Test
    void disabledByProperty() {
        contextRunner.withPropertyValues("company.logging.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LoggingFilter.class));
    }

    @Test
    void createsRestClientLoggingInterceptorWhenRestClientOnClasspath() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RestClientLoggingInterceptor.class));
    }

    @Test
    void registersRestClientCustomizerSoInterceptorAppliesAutomatically() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(org.springframework.boot.restclient.RestClientCustomizer.class));
    }

    @Configuration
    static class CustomLoggingServiceConfig {
        @Bean
        LoggingService loggingService() {
            return new CustomLoggingService();
        }
    }

    static class CustomLoggingService implements LoggingService {
        @Override
        public void logRequestResponse(final LoggingVo loggingVo) {
        }

        @Override
        public void clearRequestResponse() {
        }
    }
}