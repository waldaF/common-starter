package com.company.commonstarter.error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestErrorHandlerTest {

    private final RestErrorHandler handler = new RestErrorHandler();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(handler)
            .build();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        ((Logger) LoggerFactory.getLogger(RestErrorHandler.class)).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(RestErrorHandler.class)).detachAppender(appender);
    }

    @Test
    void validationErrorReturnsBadRequestWithFieldDetails() throws Exception {
        mockMvc.perform(post("/things").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details[0]").value("name: must not be blank"));
    }

    @Test
    void typeMismatchReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/things").param("id", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void bodyValidationIncludesObjectLevelErrorsNotJustFieldErrors() throws Exception {
        final BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new ThingRequest("ok"), "thingRequest");
        bindingResult.reject("mismatch", "fields must be consistent");
        final MethodParameter methodParameter = new MethodParameter(
                TestController.class.getDeclaredMethod("create", ThingRequest.class), 0);
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        final ResponseEntity<ApiError> response = handler.handleBodyValidation(ex);

        assertThat(response.getBody().details()).containsExactly("fields must be consistent");
    }

    @Test
    void constraintViolationUsesLeafFieldNameNotFullPropertyPath() {
        final ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("create.request.name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        final ResponseEntity<ApiError> response =
                handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getBody().details()).containsExactly("name: must not be blank");
    }

    @Test
    void methodNotAllowedReturnsStaticMessageRegardlessOfExceptionText() throws Exception {
        mockMvc.perform(delete("/things"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method not allowed"));
    }

    @Test
    void subclassesCanEnrichTypeMismatchAndNotReadableViaHooks() throws Exception {
        final RestErrorHandler customizing = new RestErrorHandler() {
            @Override
            protected String typeMismatchMessage(final MethodArgumentTypeMismatchException ex) {
                return "custom: " + ex.getName();
            }

            @Override
            protected List<String> notReadableDetails(final HttpMessageNotReadableException ex) {
                return List.of("custom detail");
            }
        };
        final MockMvc customMockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(customizing)
                .build();

        customMockMvc.perform(get("/things").param("id", "not-a-number"))
                .andExpect(jsonPath("$.message").value("custom: id"));
        customMockMvc.perform(post("/things").contentType(MediaType.APPLICATION_JSON).content("not-json"))
                .andExpect(jsonPath("$.details[0]").value("custom detail"));
    }

    @Test
    void restClientResponseWithServerErrorMapsToServiceUnavailable() {
        final RestClientResponseException ex = restClientException(503, "upstream down");

        final ResponseEntity<ApiError> response = handler.handleRestClientResponse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).isEqualTo("Service unavailable");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void restClientResponseWith4xxPassesThroughStatusCodeAndBody() {
        final RestClientResponseException ex = restClientException(404, "not found upstream");

        final ResponseEntity<ApiError> response = handler.handleRestClientResponse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errorCode()).isEqualTo(new UpstreamErrorCode(HttpStatus.NOT_FOUND));
        assertThat(response.getBody().message()).isEqualTo("Upstream rejected the request with status 404");
        assertThat(response.getBody().details()).containsExactly("Upstream response (status 404): not found upstream");
    }

    @Test
    void restClientResponseWithUnmappedClientErrorKeepsExactUpstreamStatusAndCode() {
        final RestClientResponseException ex = restClientException(422, "unprocessable");

        final ResponseEntity<ApiError> response = handler.handleRestClientResponse(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().errorCode()).isEqualTo(new UpstreamErrorCode(HttpStatus.UNPROCESSABLE_CONTENT));
        assertThat(response.getBody().message()).isEqualTo("Upstream rejected the request with status 422");
    }

    @Test
    void restClientResponseWithUnrecognized5xxCollapsesToBadGatewayInsteadOfMirroringUpstream() {
        final RestClientResponseException ex = restClientException(500, "db down");

        final ResponseEntity<ApiError> response = handler.handleRestClientResponse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.BAD_GATEWAY);
    }

    @Test
    void unmappedUpstreamErrorCodeSerializesAsPlainStringLikeCommonErrorCode() throws Exception {
        final RestErrorHandler controllerAdvice = new RestErrorHandler();
        final MockMvc errorMockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(controllerAdvice)
                .build();

        errorMockMvc.perform(get("/things/upstream-422"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("UNPROCESSABLE_CONTENT"))
                .andExpect(jsonPath("$.errorCode").isString());
    }

    @Test
    void resourceAccessExceptionMapsToServiceUnavailable() {
        final ResponseEntity<ApiError> response = handler.handleResourceAccess(new ResourceAccessException("connection refused"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).isEqualTo("Service unavailable");
    }

    @Test
    void genericRestClientExceptionMapsToUnknown() {
        final ResponseEntity<ApiError> response = handler.handleRestClient(new RestClientException("boom"));

        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.UNKNOWN);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    private static RestClientResponseException restClientException(final int status, final String body) {
        return new RestClientResponseException("upstream call failed", status, "",
                new HttpHeaders(), body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void applicationExceptionWithConsumerDefinedErrorCodeIsHandledGenerically() throws Exception {
        mockMvc.perform(get("/things/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("already exists"));
    }

    @Test
    void applicationExceptionWith5xxCodeLogsAtErrorLevel() throws Exception {
        mockMvc.perform(get("/things/boom-5xx")).andExpect(status().isInternalServerError());

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    void applicationExceptionWith4xxCodeLogsAtWarnLevel() throws Exception {
        mockMvc.perform(get("/things/boom-4xx")).andExpect(status().isBadRequest());

        assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.WARN);
    }

    @Test
    void unhandledExceptionFallsBackToUnknown() throws Exception {
        mockMvc.perform(get("/things/boom-unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("UNKNOWN"));
    }

    // Stands in for a consumer's own domain ErrorCode enum (e.g. idp-provider, e2e-service) --
    // RestErrorHandler.handleApplicationException handles it without knowing this type exists.
    private enum DomainErrorCode implements ErrorCode {
        CONFLICT(HttpStatus.CONFLICT);

        private final HttpStatus status;

        DomainErrorCode(final HttpStatus status) {
            this.status = status;
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }
    }

    @RestController
    private static class TestController {

        @PostMapping("/things")
        String create(@Valid @RequestBody final ThingRequest request) {
            return "ok";
        }

        @GetMapping("/things")
        String get(@RequestParam final int id) {
            return "ok";
        }

        @GetMapping("/things/conflict")
        String conflict() {
            throw new ApplicationException(DomainErrorCode.CONFLICT, "already exists");
        }

        @GetMapping("/things/boom-5xx")
        String boom5xx() {
            throw new ApplicationException(CommonErrorCode.UNKNOWN, "server broke");
        }

        @GetMapping("/things/boom-4xx")
        String boom4xx() {
            throw new ApplicationException(CommonErrorCode.BAD_REQUEST, "bad input");
        }

        @GetMapping("/things/boom-unexpected")
        String boomUnexpected() {
            throw new IllegalStateException("totally unexpected");
        }

        @GetMapping("/things/upstream-422")
        String upstream422() {
            throw new RestClientResponseException("upstream call failed", 422, "", new HttpHeaders(),
                    "unprocessable".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        }
    }

    private record ThingRequest(@NotBlank String name) {
    }
}