package com.company.commonstarter.swagger;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

// Seeds springdoc.* with the defaults this org always wants once company.swagger.enabled=true,
// Added as the lowest-priority property source, so any springdoc.*
// value a service sets explicitly (application.properties, env var, etc.) still wins.
public class SwaggerSpringDocDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "commonStarterSwaggerDefaults";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        if (!environment.getProperty("company.swagger.enabled", Boolean.class, false)) {
            return;
        }

        final Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("springdoc.api-docs.path", "/api-docs");
        defaults.put("springdoc.swagger-ui.path", "/swagger-ui.html");
        defaults.put("springdoc.swagger-ui.enabled", true);
        defaults.put("springdoc.swagger-ui.disable-swagger-default-url", true);
        defaults.put("springdoc.swagger-ui.display-request-duration", true);

        final String packagesToScan = environment.getProperty("company.swagger.packages-to-scan");
        if (StringUtils.hasText(packagesToScan)) {
            defaults.put("springdoc.packages-to-scan", packagesToScan);
        }

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
}