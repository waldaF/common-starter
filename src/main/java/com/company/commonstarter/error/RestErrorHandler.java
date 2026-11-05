package com.company.commonstarter.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;

// RestClient failures (5xx, timeout, connection refused) are generic to any service, so they're
// handled here; vendor-specific exceptions (S3, Elasticsearch...) stay in the consuming service.
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RestErrorHandler {

    private static final int MAX_LOGGED_BODY_LENGTH = 2000;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(final MethodArgumentNotValidException ex) {
        final List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(RestErrorHandler::formatError)
                .toList();
        return respond(CommonErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(final BindException ex) {
        final List<String> details = ex.getAllErrors().stream()
                .map(RestErrorHandler::formatError)
                .toList();
        return respond(CommonErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(final ConstraintViolationException ex) {
        final List<String> details = ex.getConstraintViolations().stream()
                .map(RestErrorHandler::formatViolation)
                .toList();
        return respond(CommonErrorCode.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: parameter '{}' has an invalid value", ex.getName());
        return respond(CommonErrorCode.BAD_REQUEST, typeMismatchMessage(ex), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(final HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());
        return respond(CommonErrorCode.BAD_REQUEST, "Malformed request body", notReadableDetails(ex));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(final HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return respond(CommonErrorCode.METHOD_NOT_ALLOWED, "Method not allowed", List.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(final Exception ex) {
        log.warn("No handler found: {}", ex.getMessage());
        return respond(CommonErrorCode.NOT_FOUND, "Not found", List.of());
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiError> handleMissingCookie(final MissingRequestCookieException ex) {
        log.warn("Missing authentication cookie: {}", ex.getCookieName());
        return respond(CommonErrorCode.UNAUTHORIZED, "Authentication required", List.of());
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> handleRestClientResponse(final RestClientResponseException ex) {
        final HttpStatusCode upstreamStatus = ex.getStatusCode();
        final String body = ex.getResponseBodyAsString();
        if (upstreamStatus.is5xxServerError()) {
            log.error("Upstream error: status={}, body={}", upstreamStatus.value(), truncate(body), ex);
        } else {
            log.warn("Upstream responded: status={}, body={}", upstreamStatus.value(), truncate(body));
        }
        final ErrorCode code = mapUpstreamStatus(upstreamStatus.value());
        final String message = code instanceof CommonErrorCode commonErrorCode
                ? commonErrorCode.getDefaultMessage()
                : "Upstream rejected the request with status " + upstreamStatus.value();
        return respond(code, message, upstreamErrorDetails(code, upstreamStatus, body));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleResourceAccess(final ResourceAccessException ex) {
        log.error("Resource access failed: {}", ex.getMessage(), ex);
        return respond(CommonErrorCode.SERVICE_UNAVAILABLE, "Service unavailable", List.of());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleRestClient(final RestClientException ex) {
        log.error("REST client error: {}", ex.getMessage(), ex);
        return respond(CommonErrorCode.UNKNOWN, "An unexpected error occurred", List.of());
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(final ApplicationException ex) {
        final ErrorCode code = ex.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.error(ex.getMessage(), ex);
        } else {
            log.warn(ex.getMessage());
        }
        return respond(code, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(final Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(CommonErrorCode.UNKNOWN, "An unexpected error occurred", List.of());
    }

    protected ResponseEntity<ApiError> respond(final ErrorCode code, final String message, final List<String> details) {
        return ResponseEntity.status(code.getStatus()).body(ApiError.of(code, message, details));
    }

    protected String typeMismatchMessage(final MethodArgumentTypeMismatchException ex) {
        return "Parameter '" + ex.getName() + "' has an invalid value";
    }

    protected List<String> notReadableDetails(final HttpMessageNotReadableException ex) {
        return List.of();
    }

    private static String formatError(final ObjectError error) {
        return error instanceof FieldError fieldError
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : error.getDefaultMessage();
    }

    private static String formatViolation(final ConstraintViolation<?> violation) {
        final String path = violation.getPropertyPath().toString();
        final int lastDot = path.lastIndexOf('.');
        final String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return field + ": " + violation.getMessage();
    }

    private static String truncate(final String body) {
        if (Objects.isNull(body) || body.length() <= MAX_LOGGED_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_LOGGED_BODY_LENGTH) + "...(truncated)";
    }

    // 503 is the one 5xx we don't collapse to BAD_GATEWAY below (it's specifically retryable); every
    // 4xx passes through as its real status via UpstreamErrorCode instead of a generic bucket.
    private static ErrorCode mapUpstreamStatus(final int status) {
        final HttpStatus resolved = HttpStatus.resolve(status);
        if (resolved == HttpStatus.SERVICE_UNAVAILABLE) {
            return CommonErrorCode.SERVICE_UNAVAILABLE;
        }
        if (Objects.nonNull(resolved) && resolved.is4xxClientError()) {
            return new UpstreamErrorCode(resolved);
        }
        return CommonErrorCode.BAD_GATEWAY;
    }

    private static List<String> upstreamErrorDetails(final ErrorCode code, final HttpStatusCode status, final String body) {
        if (!code.getStatus().is4xxClientError()) {
            return List.of();
        }
        return List.of(StringUtils.hasText(body)
                ? "Upstream response (status " + status.value() + "): " + truncate(body)
                : "Upstream responded with status " + status.value());
    }
}