package com.company.commonstarter.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    private enum TestErrorCode implements ErrorCode {
        BAD_REQUEST(HttpStatus.BAD_REQUEST);

        private final HttpStatus status;

        TestErrorCode(final HttpStatus status) {
            this.status = status;
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }
    }

    @Test
    void normalizesNullDetailsToEmptyList() {
        final ApiError error = new ApiError(TestErrorCode.BAD_REQUEST, "bad", null);
        assertThat(error.details()).isEmpty();
    }

    @Test
    void ofWithoutDetailsDefaultsToEmptyList() {
        final ApiError error = ApiError.of(TestErrorCode.BAD_REQUEST, "bad");
        assertThat(error.details()).isEmpty();
    }

    @Test
    void ofWithDetailsKeepsThem() {
        final ApiError error = ApiError.of(TestErrorCode.BAD_REQUEST, "bad", java.util.List.of("field x invalid"));
        assertThat(error.details()).containsExactly("field x invalid");
    }
}