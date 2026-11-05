package com.company.commonstarter.error;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    public ApplicationException(final ErrorCode errorCode, final String message) {
        this(errorCode, message, List.of());
    }

    public ApplicationException(final ErrorCode errorCode, final String message, final List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = Objects.isNull(details) ? List.of() : details;
    }

    public ApplicationException(final ErrorCode errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = List.of();
    }
}