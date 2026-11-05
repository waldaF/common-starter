package com.company.commonstarter.error;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorCode unauthorizedErrorCode;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final AuthenticationException authException) throws IOException {
        response.setStatus(unauthorizedErrorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiError.of(unauthorizedErrorCode, authException.getMessage()));
    }
}