package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.config.ErrorHandlingProperties;
import com.company.commonstarter.error.RestErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorHandlingAutoConfiguration.class));

    @Test
    void bindsPropertiesByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(ErrorHandlingProperties.class).isEnabled()).isTrue());
    }

    @Test
    void registersSharedRestErrorHandlerByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(RestErrorHandler.class));
    }

    @Test
    void disabledByProperty() {
        contextRunner.withPropertyValues("company.error-handling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ErrorHandlingProperties.class);
                    assertThat(context).doesNotHaveBean(RestErrorHandler.class);
                });
    }

    @Test
    void backsOffWhenConsumerDefinesOwnRestErrorHandler() {
        contextRunner.withUserConfiguration(CustomHandlerConfig.class)
                .run(context -> assertThat(context.getBean(RestErrorHandler.class))
                        .isSameAs(CustomHandlerConfig.CUSTOM_HANDLER));
    }

    @Configuration
    static class CustomHandlerConfig {

        static final RestErrorHandler CUSTOM_HANDLER = new RestErrorHandler();

        @Bean
        RestErrorHandler restErrorHandler() {
            return CUSTOM_HANDLER;
        }
    }
}