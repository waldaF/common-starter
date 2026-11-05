package com.company.commonstarter.config;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "company.jwt")
public class JwtClientProperties {

    private boolean enabled = false;

    // Named targets that trust different secrets, e.g. company.jwt.clients.order-service.secret=...
    private Map<String, @Valid JwtClientConfig> clients = new LinkedHashMap<>();
}