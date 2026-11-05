package com.company.commonstarter.config;

import lombok.experimental.UtilityClass;

import java.util.List;

// shared default for anything path-pattern-based that wants to treat framework/infra endpoints
// (health, metrics, Swagger) specially -- used by both LoggingProperties.excludePathPatterns and
// SecurityProperties.publicPaths, so the two don't drift out of sync for the common case.
@UtilityClass
public class FrameworkPaths {

    public static final List<String> DEFAULT = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            // non-default springdoc.api-docs.path some services configure instead of /v3/api-docs
            "/api-docs/**",
            "/webjars/**",
            "/favicon.ico"
    );
}