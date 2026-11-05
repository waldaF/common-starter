package com.company.commonstarter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Only enforces the credential check for protectedPaths; publicPaths always bypass it even if a
// protectedPaths pattern would also match, and everything else falls through to
// SecurityFilterChain's anyRequest().authenticated() with no Authentication set.
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_USER_ROLE = "ROLE_API_USER";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SecurityProperties properties;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        if (properties.getAuthType() == SecurityAuthType.NONE || shouldBypass(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        final String credential = extractCredential(request);
        if (!StringUtils.hasText(credential) || !properties.getCredentials().contains(credential)) {
            throw new BadCredentialsException("Invalid or missing credentials");
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                credential, null, List.of(new SimpleGrantedAuthority(API_USER_ROLE))));
        filterChain.doFilter(request, response);
    }

    private String extractCredential(final HttpServletRequest request) {
        if (properties.getAuthType() == SecurityAuthType.BEARER) {
            final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            return header != null && header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()) : null;
        }
        return request.getHeader(properties.getHeaderName());
    }

    private boolean shouldBypass(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || matchesAny(properties.getEffectivePublicPaths(), path)
                || !matchesAny(properties.getEffectiveProtectedPaths(), path);
    }

    private static boolean matchesAny(final List<String> patterns, final String path) {
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}