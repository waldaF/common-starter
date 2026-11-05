package com.company.commonstarter.security;

import com.company.commonstarter.config.FrameworkPaths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.security")
@Validated
public class SecurityProperties {
    private boolean enabled = false;
    private SecurityAuthType authType = SecurityAuthType.CUSTOM_HEADER;
    // only used when authType == CUSTOM_HEADER
    private String headerName = "X-Api-Key";
    // valid credential values for BEARER (token) and CUSTOM_HEADER (key); unused for NONE
    private Set<String> credentials = Set.of();
    private List<String> publicPaths = FrameworkPaths.DEFAULT;
    private List<String> protectedPaths = List.of("/api/**");

    private List<String> additionalPublicPaths = List.of();
    private List<String> additionalProtectedPaths = List.of();

    public List<String> getEffectivePublicPaths() {
        return Stream.concat(publicPaths.stream(), additionalPublicPaths.stream())
                .distinct()
                .toList();
    }

    public List<String> getEffectiveProtectedPaths() {
        return Stream.concat(protectedPaths.stream(), additionalProtectedPaths.stream())
                .distinct()
                .toList();
    }
}