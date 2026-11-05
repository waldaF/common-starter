package com.company.commonstarter.config;

import com.company.commonstarter.jwt.client.JwtAuthScheme;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
public class JwtClientConfig {

    @Size(min = 20, message = "JWT secret must be at least 20 characters")
    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    @Min(value = 5, message = "JWT expiration must be at least 5 minutes")
    @Max(value = 60, message = "JWT expiration must not exceed 60 minutes")
    private int expirationInMinutes = 15;

    @Min(value = 1, message = "JWT expiration refresh threshold must be at least 1 minute")
    @Max(value = 60, message = "JWT expiration refresh threshold must not exceed 60 minutes")
    private int expirationRefreshBeforeInMinutes = 2;

    private JwtAuthScheme authScheme = JwtAuthScheme.BEARER;
}