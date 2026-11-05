package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.correlation.CorrelationIdProperties;
import com.company.commonstarter.correlation.CorrelationIdFilter;
import com.company.commonstarter.correlation.CorrelationIdPropagationInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnProperty(prefix = "company.correlation-id", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CorrelationIdProperties.class)
public class CorrelationIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter(final CorrelationIdProperties properties) {
        return new CorrelationIdFilter(properties);
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(final CorrelationIdFilter correlationIdFilter) {
        final FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(correlationIdFilter);
        registration.setOrder(FilterOrder.CORRELATION_ID);
        return registration;
    }

    // Nested so a missing spring-boot-restclient on a consumer's classpath only skips this
    // config instead of crashing the whole autoconfiguration class on reflection.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    static class RestClientCorrelationIdConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public CorrelationIdPropagationInterceptor correlationIdPropagationInterceptor(final CorrelationIdProperties properties) {
            return new CorrelationIdPropagationInterceptor(properties);
        }

        @Bean
        public RestClientCustomizer correlationIdRestClientCustomizer(final CorrelationIdPropagationInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }
}