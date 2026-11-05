package com.company.commonstarter.jwt.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtClientInterceptorTest {

    private static final int EXPIRATION_IN_MINUTES = 10;
    private static final int EXPIRATION_REFRESH_BEFORE_IN_MINUTES = 2;
    private static final String SECRET = "test-secret-key-for-jwt-generation-min-32-chars";

    private JwtClientInterceptor interceptor;

    static Stream<Arguments> tokenRegenerationScenarios() {
        return Stream.of(
                Arguments.of("expired (past expiration)", -10L),
                Arguments.of("at refresh threshold (1 minute from now)", 1L),
                Arguments.of("within refresh threshold (0 minutes from now)", 0L));
    }

    static Stream<Arguments> tokenNoRegenerationScenarios() {
        final int refreshThresholdMinutes = EXPIRATION_IN_MINUTES - EXPIRATION_REFRESH_BEFORE_IN_MINUTES;
        return Stream.of(
                Arguments.of("at refresh threshold (" + refreshThresholdMinutes + " minutes from now)", (long) refreshThresholdMinutes),
                Arguments.of("above refresh threshold (9 minutes from now)", 9L),
                Arguments.of("at full expiration (" + EXPIRATION_IN_MINUTES + " minutes from now)", (long) EXPIRATION_IN_MINUTES));
    }

    @BeforeEach
    void setup() {
        final JwtClientSettings settings = new JwtClientSettings(SECRET, EXPIRATION_IN_MINUTES, EXPIRATION_REFRESH_BEFORE_IN_MINUTES);
        interceptor = new JwtClientInterceptor(settings, "test", JwtAuthScheme.X_API_AUTHORIZATION);
    }

    @Test
    void usesBearerSchemeWhenProvided() throws Exception {
        final JwtClientSettings settings = new JwtClientSettings(SECRET, EXPIRATION_IN_MINUTES, EXPIRATION_REFRESH_BEFORE_IN_MINUTES);
        final JwtClientInterceptor customInterceptor = new JwtClientInterceptor(settings, "test", JwtAuthScheme.BEARER);

        final HttpRequest request = mock(HttpRequest.class);
        final HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
        final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        customInterceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
        assertThat(headers.containsHeader(JwtAuthScheme.X_API_AUTHORIZATION.getHeaderName())).isFalse();
    }

    @Test
    void returnsCachedTokenWhenValid() {
        final String t1 = interceptor.getValidToken();
        final String t2 = interceptor.getValidToken();
        assertThat(t2).isEqualTo(t1);
    }

    @Test
    void padsShortSecretsToHs256MinimumKeySize() {
        final JwtClientSettings shortSecret = new JwtClientSettings("short", EXPIRATION_IN_MINUTES, EXPIRATION_REFRESH_BEFORE_IN_MINUTES);
        final JwtClientInterceptor withShortSecret = new JwtClientInterceptor(shortSecret, "test", JwtAuthScheme.BEARER);

        assertThat(withShortSecret.getValidToken()).isNotBlank();
    }

    @ParameterizedTest(name = "Should regenerate token when {0}")
    @MethodSource("tokenRegenerationScenarios")
    void getValidToken_shouldRegenerateWhenInvalid(final String scenario, final long minutesFromNow) throws InterruptedException {
        final String initialToken = interceptor.getValidToken();
        assertThat(initialToken).isNotNull();
        final Instant expirationTime = Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES);
        ReflectionTestUtils.setField(interceptor, "tokenExpiration", expirationTime);
        ReflectionTestUtils.setField(interceptor, "cachedToken", initialToken);
        Thread.sleep(1000);

        final String newToken = interceptor.getValidToken();
        final Instant expirationAfter = (Instant) ReflectionTestUtils.getField(interceptor, "tokenExpiration");

        assertThat(expirationAfter).as("Expiration should be updated for: " + scenario).isNotEqualTo(expirationTime);
        assertThat(newToken).as("New token should differ for: " + scenario).isNotEqualTo(initialToken);
    }

    @ParameterizedTest(name = "Should NOT regenerate token when {0}")
    @MethodSource("tokenNoRegenerationScenarios")
    void getValidToken_shouldNotRegenerateWhenValid(final String scenario, final long minutesFromNow) {
        final String initialToken = interceptor.getValidToken();
        assertThat(initialToken).isNotNull();
        final Instant expirationTime = Instant.now().plus(minutesFromNow, ChronoUnit.MINUTES);
        ReflectionTestUtils.setField(interceptor, "tokenExpiration", expirationTime);
        ReflectionTestUtils.setField(interceptor, "cachedToken", initialToken);

        final String newToken = interceptor.getValidToken();
        final Instant expirationAfter = (Instant) ReflectionTestUtils.getField(interceptor, "tokenExpiration");

        assertThat(expirationAfter).as("Expiration should NOT change for: " + scenario).isEqualTo(expirationTime);
        assertThat(newToken).as("Token should NOT regenerate for: " + scenario).isEqualTo(initialToken);
    }
}