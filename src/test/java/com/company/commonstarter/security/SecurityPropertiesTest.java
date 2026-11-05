package com.company.commonstarter.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPropertiesTest {

    @Test
    void effectivePathsUnionBaseAndAdditionalWithoutRequiringTheBaseListToBeRepeated() {
        final SecurityProperties properties = new SecurityProperties();
        properties.setAdditionalPublicPaths(List.of("/custom-health"));
        properties.setAdditionalProtectedPaths(List.of("/actuator/externals"));

        assertThat(properties.getEffectivePublicPaths())
                .contains("/actuator/health", "/swagger-ui/**", "/custom-health");
        assertThat(properties.getEffectiveProtectedPaths())
                .containsExactlyInAnyOrder("/api/**", "/actuator/externals");
    }

    @Test
    void effectivePathsAreJustTheBaseListWhenNoAdditionsAreConfigured() {
        final SecurityProperties properties = new SecurityProperties();

        assertThat(properties.getEffectiveProtectedPaths()).containsExactly("/api/**");
    }
}