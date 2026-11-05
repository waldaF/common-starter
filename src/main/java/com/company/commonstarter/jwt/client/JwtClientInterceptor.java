package com.company.commonstarter.jwt.client;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
public final class JwtClientInterceptor implements ClientHttpRequestInterceptor {

    // HS256 requires a key of at least 256 bits (RFC 7518) -- shorter secrets are zero-padded up to this length
    private static final int HS256_MIN_KEY_BYTES = 32;

    private final JwtClientSettings settings;
    private final String clientLabel;
    private final JwtAuthScheme authScheme;
    private final ReentrantLock tokenLock = new ReentrantLock();
    private String cachedToken;
    private Instant tokenExpiration;

    @Override
    public ClientHttpResponse intercept(final HttpRequest request,
                                        final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(authScheme.getHeaderName(), authScheme.headerValue(getValidToken()));
        log.debug("Added JWT for {} to request: {}", clientLabel, request.getURI());
        return execution.execute(request, body);
    }

    public String getValidToken() {
        tokenLock.lock();
        try {
            if (isTokenValid()) {
                return cachedToken;
            }
            log.debug("Generating new JWT token for {}", clientLabel);
            final Instant now = Instant.now();
            final Instant expiration = now.plus(settings.expirationInMinutes(), ChronoUnit.MINUTES);
            final Map<String, Object> claims = Map.of(
                    "exp", expiration.getEpochSecond(),
                    "iat", now.getEpochSecond());
            cachedToken = Jwts.builder()
                    .claims(claims)
                    .signWith(generateKey(), Jwts.SIG.HS256)
                    .compact();
            tokenExpiration = expiration;
            log.debug("JWT for {} generated, expires at: {}", clientLabel, tokenExpiration);
            return cachedToken;
        } finally {
            tokenLock.unlock();
        }
    }

    private boolean isTokenValid() {
        if (Objects.isNull(cachedToken) || Objects.isNull(tokenExpiration)) {
            return false;
        }
        final Instant now = Instant.now();
        final int expMin = settings.expirationInMinutes();
        final int refreshMin = settings.expirationRefreshBeforeInMinutes();
        final int refreshBefore = Math.min(refreshMin, expMin - 1);
        final Instant refreshThreshold = tokenExpiration.minus(refreshBefore, ChronoUnit.MINUTES);
        return now.isBefore(refreshThreshold);
    }

    private SecretKey generateKey() {
        try {
            byte[] secretBytes = settings.secret().getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < HS256_MIN_KEY_BYTES) {
                final byte[] paddedKey = new byte[HS256_MIN_KEY_BYTES];
                System.arraycopy(secretBytes, 0, paddedKey, 0, secretBytes.length);
                secretBytes = paddedKey;
            }
            return Keys.hmacShaKeyFor(secretBytes);
        } catch (WeakKeyException e) {
            throw new JwtKeyGenerationException("Error during generate JWT key", e);
        }
    }
}