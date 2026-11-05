package com.company.commonstarter.error;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

// Wraps an upstream HTTP status with no dedicated CommonErrorCode, so the real status is preserved
// instead of being flattened to a generic bucket.
public record UpstreamErrorCode(HttpStatus status) implements ErrorCode {

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    // keeps errorCode a plain string like CommonErrorCode, not a nested object
    @JsonValue
    public String name() {
        return status.name();
    }
}