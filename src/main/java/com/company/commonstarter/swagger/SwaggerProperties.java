package com.company.commonstarter.swagger;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.swagger")
@Validated
public class SwaggerProperties {
    private boolean enabled = false;
    @NotNull
    private SwaggerAuthType authType = SwaggerAuthType.NONE;
    private String oauth2TokenUrl = "";
    private String apiKeyHeaderName = "X-Api-Key";
    private String serverUrl;
    @NotBlank(message = "Team name cannot be blank")
    private String team;
    @NotBlank(message = "Team url can not be blank")
    private String teamUrl;
    // Forwarded to springdoc.packages-to-scan; leave unset to let springdoc scan everything.
    private String packagesToScan;
}