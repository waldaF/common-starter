package com.company.commonstarter.logging;

import com.company.commonstarter.config.LoggingProperties;
import com.company.commonstarter.masking.TextUpdater;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final LoggingProperties properties;
    private final LoggingService loggingService;
    private final List<TextUpdater> textUpdaters;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final FilterChain filterChain) throws ServletException, IOException {

        final long startTime = System.currentTimeMillis();
        final ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, properties.getMaxBodyChars());
        final ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            if (!isExcluded(request)) {
                logRequest(request, wrappedRequest, wrappedResponse, startTime);
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean isExcluded(final HttpServletRequest request) {
        return PathMatchUtils.matchesAny(properties.getExcludePathPatterns(), request.getRequestURI());
    }

    private void logRequest(final HttpServletRequest request,
                             final ContentCachingRequestWrapper wrappedRequest,
                             final ContentCachingResponseWrapper wrappedResponse,
                             final long startTime) {
        final int status = wrappedResponse.getStatus();
        final long duration = System.currentTimeMillis() - startTime;
        final boolean isError = status >= HttpStatus.INTERNAL_SERVER_ERROR.value()
                || properties.getErrorStatusCodes().contains(status);

        final LoggingVo loggingVo = LoggingVo.builder()
                .status(status)
                .requestHeaders(LoggingTextSupport.sanitize(
                        HeaderUtils.extractHeadersFromRequest(wrappedRequest), textUpdaters))
                .requestURI(request.getRequestURI())
                .requestBody(LoggingTextSupport.limitAndSanitize(
                        wrappedRequest.getContentAsByteArray(), properties.getMaxBodyChars(), textUpdaters))
                .responseBody(LoggingTextSupport.limitAndSanitize(
                        wrappedResponse.getContentAsByteArray(), properties.getMaxBodyChars(), textUpdaters))
                .method(request.getMethod())
                .duration(duration)
                .error(isError)
                .build();

        loggingService.logRequestResponse(loggingVo);
    }
}