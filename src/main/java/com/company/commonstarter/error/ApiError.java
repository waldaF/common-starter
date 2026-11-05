package com.company.commonstarter.error;

import java.util.List;
import java.util.Objects;

public record ApiError(ErrorCode errorCode, String message, List<String> details) {

    public ApiError {
        details = Objects.isNull(details) ? List.of() : details;
    }

    public static ApiError of(final ErrorCode errorCode, final String message) {
        return new ApiError(errorCode, message, List.of());
    }

    public static ApiError of(final ErrorCode errorCode, final String message, final List<String> details) {
        return new ApiError(errorCode, message, details);
    }
}