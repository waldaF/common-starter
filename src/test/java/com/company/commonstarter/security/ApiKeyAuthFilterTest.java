package com.company.commonstarter.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApiKeyAuthFilterTest {

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customHeaderWithValidKeyAuthenticates() throws Exception {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.CUSTOM_HEADER);
        final MockHttpServletRequest request = protectedRequest();
        request.addHeader("X-Api-Key", "secret");

        new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("secret");
        verify(chain).doFilter(request, response);
    }

    @Test
    void customHeaderWithWrongKeyIsRejected() {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.CUSTOM_HEADER);
        final MockHttpServletRequest request = protectedRequest();
        request.addHeader("X-Api-Key", "wrong");

        assertThatThrownBy(() -> new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void bearerTokenIsExtractedFromAuthorizationHeader() throws Exception {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.BEARER);
        final MockHttpServletRequest request = protectedRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer secret");

        new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("secret");
    }

    @Test
    void bearerModeRejectsNonBearerAuthorizationHeader() {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.BEARER);
        final MockHttpServletRequest request = protectedRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic secret");

        assertThatThrownBy(() -> new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void noneModeNeverChecksCredentials() throws Exception {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.NONE);
        final MockHttpServletRequest request = protectedRequest();

        new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void publicPathBypassesEvenWithoutCredentials() throws Exception {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.CUSTOM_HEADER);
        properties.setPublicPaths(List.of("/actuator/health"));
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void pathOutsideProtectedPatternsBypassesWithoutAuthenticating() throws Exception {
        final SecurityProperties properties = propertiesWith(SecurityAuthType.CUSTOM_HEADER);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unlisted");

        new ApiKeyAuthFilter(properties).doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    private static SecurityProperties propertiesWith(final SecurityAuthType authType) {
        final SecurityProperties properties = new SecurityProperties();
        properties.setAuthType(authType);
        properties.setCredentials(Set.of("secret"));
        properties.setProtectedPaths(List.of("/api/**"));
        return properties;
    }

    private static MockHttpServletRequest protectedRequest() {
        return new MockHttpServletRequest("GET", "/api/things");
    }
}