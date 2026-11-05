# common-starter

## 1. Description

Shared Spring Boot auto-configuration starter for Spring Boot services: correlation-id propagation,
structured logging, log masking, standardized REST error handling, a self-signing JWT client
interceptor, and optional Swagger/OpenAPI setup. Everything is wired via `@AutoConfiguration` — just
add properties to switch features on/off or tweak behaviour.

## 2. Build and test

Requirements: Java 25, Maven 3.9+.

```
mvn clean install   # build the jar and run all tests
mvn test            # run tests only
```

## 3. Technologies

- Java 25
- Spring Boot 4.1.0 / Spring Framework 7 (`spring-boot-starter`, `spring-boot-autoconfigure`,
  `spring-boot-starter-webmvc`, `spring-boot-restclient`, `spring-boot-starter-security`,
  `spring-boot-starter-validation`)
- Lombok
- Jackson (`jackson-databind`)
- Logback (`logback-classic`)
- jjwt 0.13.0 (client JWT signing)
- springdoc-openapi 3.1.1 (optional, Swagger/OpenAPI UI)

## 4. Usage

Add the dependency to your service's `pom.xml`, then configure via `application.properties`/`.yml`.
Every property below is prefixed with `company.`.

### Correlation ID (enabled by default)

```properties
# default: true - disable to turn off the filter
company.correlation-id.enabled=true
# default: X-Correlation-Id - the header name
company.correlation-id.header-name=X-Correlation-Id
# default: correlation_id - the MDC log key
company.correlation-id.mdc-key=correlation_id
```

### Logging (enabled by default)

```properties
# default: true - disable to turn off request/response logging
company.logging.enabled=true
# default: false - true for readable single-line logs, false for JSON
company.logging.simple-console-logging=false
# default: local - shown in logs, e.g. prod, staging
company.logging.env=local
# default: empty - service name shown in logs
company.logging.app-name=
# default: 1024000 - body is truncated past this many chars
company.logging.max-body-chars=1024000
# default: 512000 - same, but for outbound RestClient calls
company.logging.max-outbound-body-chars=512000
# default: health/prometheus/swagger/api-docs paths - paths excluded from logging
company.logging.exclude-path-patterns=/actuator/health,/actuator/health/**,/actuator/prometheus,/swagger-ui/**,/v3/api-docs/**
# default: 401,403 - status codes also logged at ERROR level
company.logging.error-status-codes=401,403
```

### Masking (enabled by default)

```properties
# default: true - disable to turn masking off entirely
company.masking.enabled=true
# default: true - also masks the request/response logging above
company.masking.apply-to-http-logging=true
# default: empty - add as many rules as you need via rules[0], rules[1], ...
company.masking.rules[0].pattern=(?i)(ssn=)\\d+
company.masking.rules[0].replacement=$1***
company.masking.rules[1].pattern=(?i)(iban=)[A-Z0-9]+
company.masking.rules[1].replacement=$1***
```

Built-in rules (`password`/`secret`/`clientSecret` fields, `Bearer` tokens, `private-key`/`private_key`
values, `X-API-Key`/`X-Api-Authorization` headers) always apply; `masking.rules` only adds to them.

### Error handling (enabled by default)

```properties
# default: true - disable to turn off the shared @RestControllerAdvice
company.error-handling.enabled=true
```

### JWT client (disabled by default)

For calling other backends you share an HMAC secret with. `JwtClientInterceptor` self-signs a short-lived HS256 JWT and caches it, refreshing
near expiry. One `clients.<name>.*` entry per target; auto-configures a `Map<String, JwtClientInterceptor>`
bean keyed by `<name>`.

```properties
# default: false - opt-in
company.jwt.enabled=true
# required - HMAC secret shared with this target
company.jwt.clients.order-service.secret=${ORDER_SERVICE_JWT_SECRET}
# default: 15 - token lifetime in minutes
company.jwt.clients.order-service.expiration-in-minutes=30
# default: 2 - re-sign this many minutes before expiry
company.jwt.clients.order-service.expiration-refresh-before-minutes=5
company.jwt.clients.billing-service.secret=${BILLING_SERVICE_JWT_SECRET}
# default: BEARER - or X_API_AUTHORIZATION for an X-Api-Authorization header
company.jwt.clients.billing-service.auth-scheme=X_API_AUTHORIZATION
```

Build one `RestClient` per target on top of the map. Use `JwtClientInterceptors.require(...)` instead
of `Map.get(...)` — it fails fast with the exact property to fix, instead of a generic
`"Interceptor must not be null"` from `RestClient.Builder` several layers away:

```java
@Configuration
public class JwtSecuredClientsConfig {

    @Bean
    public RestClient orderServiceClient(final RestClient.Builder sharedBuilder,
                                          final Map<String, JwtClientInterceptor> jwtInterceptors) {
        return RestClientSupport.forClient(sharedBuilder, "https://order-service.internal",
                JwtClientInterceptors.require(jwtInterceptors, "order-service"));
    }

    @Bean
    public RestClient billingServiceClient(final RestClient.Builder sharedBuilder,
                                            final Map<String, JwtClientInterceptor> jwtInterceptors) {
        return RestClientSupport.forClient(sharedBuilder, "https://billing-service.internal",
                JwtClientInterceptors.require(jwtInterceptors, "billing-service"));
    }
}
```

### Security (disabled by default)

Registers a `SecurityFilterChain` with CSRF disabled, standard security headers (HSTS, CSP, frame
options, referrer policy), and a credential-checking filter for whichever paths you mark as
`protected-paths`. `public-paths` always bypass the filter, everything else requires authentication
via Spring Security's `anyRequest().authenticated()`. 401 responses are JSON via the same entry point
`company.error-handling.*` registers, so leave that enabled too.

```properties
# default: false - opt-in
company.security.enabled=true
# default: CUSTOM_HEADER - or NONE (no check) or BEARER (Authorization: Bearer <token>)
company.security.auth-type=CUSTOM_HEADER
# default: X-Api-Key - only used when auth-type=CUSTOM_HEADER
company.security.header-name=X-Api-Key
# required unless auth-type=NONE - valid keys/tokens, add as many as you need
company.security.credentials[0]=${API_KEY_1}
company.security.credentials[1]=${API_KEY_2}
# default: common health/swagger/api-docs paths - paths that skip the credential check
company.security.public-paths[0]=/some-custom-public-path/**
# default: /api/** - paths the credential check actually enforces
company.security.protected-paths[0]=/api/**
```

Setting `public-paths`/`protected-paths` **replaces** the default list above entirely — that's how
Spring Boot list binding works, it doesn't merge. If you just want to add one path on top of the
defaults without repeating the whole list, use the additive variants instead, which are unioned with
the base list at startup:

```properties
# default: empty - added to public-paths instead of replacing it
company.security.additional-public-paths[0]=/some-custom-public-path/**
# default: empty - added to protected-paths instead of replacing it
company.security.additional-protected-paths[0]=/actuator/externals
```

So a typical service only needs `enabled`, `auth-type`/`header-name`, `credentials`, and
`additional-protected-paths` for anything beyond `/api/**` — no need to touch `public-paths` at all
unless it serves something outside the common Swagger/Actuator set above.

Every property is app-to-app: common-starter only ships the defaults above, each consuming service
sets its own values to match its own routes. `public-paths` and `company.logging.exclude-path-patterns`
share the same default list but are independent properties — override either on its own if a service
needs to log a path it still keeps unauthenticated, or vice versa.

### Swagger / OpenAPI (disabled by default)

One property namespace, not two: `company.swagger.enabled=true` both builds the `OpenAPI` bean
below and seeds the springdoc defaults this org always wants (`/api-docs`, `/swagger-ui.html`,
swagger-ui enabled, default-url disabled, request duration shown) — you no longer need to mirror a
parallel set of `springdoc.*` properties in every service. Seeded values are added as the
lowest-priority property source, so any `springdoc.*` property a service sets explicitly still wins.

```properties
# default: false - opt-in, pulls in springdoc-openapi-starter-webmvc-ui
company.swagger.enabled=true
# required - shown as the API contact/owner
company.swagger.team=Platform Team
company.swagger.team-url=https://example.com/team
# default: unset - shown as the server URL in the OpenAPI doc
company.swagger.server-url=https://service.internal
# default: NONE - or JWT, BASIC, OAUTH2, API_KEY to register that security scheme
company.swagger.auth-type=OAUTH2
# only used when auth-type=OAUTH2
company.swagger.oauth2-token-url=https://auth.example.com/token
# default: X-Api-Key - only used when auth-type=API_KEY
company.swagger.api-key-header-name=X-Custom-Key
# default: unset - forwarded to springdoc.packages-to-scan
company.swagger.packages-to-scan=com.example.app.controller,com.example.app.dto,com.example.app.error
```

Only set raw `springdoc.*` properties yourself for something the `company.swagger.*` namespace
doesn't cover yet (e.g. `springdoc.show-actuator`) — anything it does cover should go through
`company.swagger.*` so the service's config stays in one place.