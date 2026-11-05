package com.company.commonstarter.jwt.client;

public class JwtKeyGenerationException extends RuntimeException {

    public JwtKeyGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}