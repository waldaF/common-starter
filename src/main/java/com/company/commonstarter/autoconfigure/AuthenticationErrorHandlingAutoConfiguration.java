package com.company.commonstarter.autoconfigure;

import tools.jackson.databind.ObjectMapper;
import com.company.commonstarter.error.CommonErrorCode;
import com.company.commonstarter.error.JsonAuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(AuthenticationEntryPoint.class)
@ConditionalOnProperty(prefix = "company.error-handling", name = "enabled", matchIfMissing = true)
public class AuthenticationErrorHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint(final ObjectMapper objectMapper) {
        return new JsonAuthenticationEntryPoint(CommonErrorCode.UNAUTHORIZED, objectMapper);
    }
}