package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.correlation.CorrelationIdFilter;
import com.company.commonstarter.correlation.CorrelationIdPropagationInterceptor;
import com.company.commonstarter.correlation.CorrelationIdProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CorrelationIdAutoConfiguration.class));

    @Test
    void createsFilterByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(CorrelationIdFilter.class));
    }

    @Test
    void disabledByProperty() {
        contextRunner.withPropertyValues("company.correlation-id.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CorrelationIdFilter.class));
    }

    @Test
    void consumerSuppliedFilterWins() {
        contextRunner.withUserConfiguration(CustomFilterConfig.class)
                .run(context -> assertThat(context.getBean(CorrelationIdFilter.class))
                        .isInstanceOf(CustomCorrelationIdFilter.class));
    }

    @Test
    void createsPropagationInterceptorWhenRestClientOnClasspath() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(CorrelationIdPropagationInterceptor.class));
    }

    @Test
    void registersRestClientCustomizerSoInterceptorAppliesAutomatically() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(org.springframework.boot.restclient.RestClientCustomizer.class));
    }

    @Configuration
    static class CustomFilterConfig {

        @Bean
        CorrelationIdFilter correlationIdFilter(CorrelationIdProperties properties) {
            return new CustomCorrelationIdFilter(properties);
        }
    }

    static class CustomCorrelationIdFilter extends CorrelationIdFilter {
        CustomCorrelationIdFilter(final CorrelationIdProperties properties) {
            super(properties);
        }
    }
}