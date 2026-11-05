package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.config.ErrorHandlingProperties;
import com.company.commonstarter.error.RestErrorHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "company.error-handling", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ErrorHandlingProperties.class)
public class ErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestErrorHandler restErrorHandler() {
        return new RestErrorHandler();
    }
}