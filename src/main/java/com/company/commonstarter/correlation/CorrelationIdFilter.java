package com.company.commonstarter.correlation;

import com.company.commonstarter.mdc.MdcUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final CorrelationIdProperties properties;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final FilterChain filterChain) throws ServletException, IOException {

        final String correlationId = getOrCreateCorrelationId(request);
        response.setHeader(properties.getHeaderName(), correlationId);
        try {
            MdcUtils.putCorrelationId(properties.getMdcKey(), correlationId);
            // ScopedValue backs MDC up across thread/context boundaries MDC alone won't cross
            CorrelationIdContext.runWithCorrelationId(correlationId,
                    () -> doFilterUnchecked(filterChain, request, response));
        } catch (CorrelationIdFilterException e) {
            if (e.getCause() instanceof ServletException servletException) {
                throw servletException;
            }
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        } finally {
            MdcUtils.removeCorrelationId(properties.getMdcKey());
        }
    }

    private void doFilterUnchecked(final FilterChain filterChain,
                                    final HttpServletRequest request,
                                    final HttpServletResponse response) {
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException e) {
            throw new CorrelationIdFilterException(e);
        }
    }

    private String getOrCreateCorrelationId(final HttpServletRequest request) {
        final String correlationId = request.getHeader(properties.getHeaderName());
        return UuidValidator.isValidUuid(correlationId) ? correlationId : UUID.randomUUID().toString();
    }

    private static final class CorrelationIdFilterException extends RuntimeException {
        private CorrelationIdFilterException(final Throwable cause) {
            super(cause);
        }
    }
}