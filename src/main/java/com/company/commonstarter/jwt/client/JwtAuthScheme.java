package com.company.commonstarter.jwt.client;

import org.springframework.http.HttpHeaders;

public enum JwtAuthScheme {

    X_API_AUTHORIZATION("X-Api-Authorization", ""),
    BEARER(HttpHeaders.AUTHORIZATION, "Bearer ");

    private final String headerName;
    private final String tokenPrefix;

    JwtAuthScheme(final String headerName, final String tokenPrefix) {
        this.headerName = headerName;
        this.tokenPrefix = tokenPrefix;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String headerValue(final String token) {
        return tokenPrefix + token;
    }
}