package com.company.commonstarter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.error-handling")
public class ErrorHandlingProperties {

    private boolean enabled = true;
}