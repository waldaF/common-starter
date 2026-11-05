package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.config.LoggingProperties;
import com.company.commonstarter.logging.ConsoleJsonMdcLoggingService;
import com.company.commonstarter.logging.ConsoleLoggingService;
import com.company.commonstarter.logging.LoggingFilter;
import com.company.commonstarter.logging.LoggingService;
import com.company.commonstarter.logging.RestClientLoggingInterceptor;
import com.company.commonstarter.masking.TextUpdater;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@AutoConfiguration(after = CorrelationIdAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "company.logging", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoggingService.class)
    @ConditionalOnProperty(prefix = "company.logging", name = "simple-console-logging",
            havingValue = "false", matchIfMissing = true)
    public LoggingService consoleJsonMdcLoggingService(final LoggingProperties properties) {
        return new ConsoleJsonMdcLoggingService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LoggingService.class)
    @ConditionalOnProperty(prefix = "company.logging", name = "simple-console-logging",
            havingValue = "true")
    public LoggingService consoleLoggingService() {
        return new ConsoleLoggingService();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingFilter loggingFilter(final LoggingProperties properties,
                                        final LoggingService loggingService,
                                        final List<TextUpdater> textUpdaters) {
        return new LoggingFilter(properties, loggingService, textUpdaters);
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration(final LoggingFilter loggingFilter) {
        final FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>(loggingFilter);
        registration.setOrder(FilterOrder.LOGGING);
        return registration;
    }

    // Nested so a missing spring-boot-restclient on a consumer's classpath only skips this
    // config instead of crashing the whole autoconfiguration class on reflection.
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestClientCustomizer.class)
    static class RestClientLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestClientLoggingInterceptor restClientLoggingInterceptor(final LoggingProperties properties,
                                                                           final LoggingService loggingService,
                                                                           final List<TextUpdater> textUpdaters) {
            return new RestClientLoggingInterceptor(properties, loggingService, textUpdaters);
        }

        @Bean
        public RestClientCustomizer loggingRestClientCustomizer(final RestClientLoggingInterceptor interceptor) {
            return builder -> builder.requestInterceptor(interceptor);
        }
    }
}