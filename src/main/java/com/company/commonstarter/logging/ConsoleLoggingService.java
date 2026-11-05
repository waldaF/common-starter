package com.company.commonstarter.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

// Human-readable single-line alternative to ConsoleJsonMdcLoggingService, for local/plain-text
// console use where there's no JSON encoder to flatten MDC fields.
@Slf4j
public class ConsoleLoggingService implements LoggingService {

    private static final String EMPTY = "{}";

    @Override
    public void logRequestResponse(final LoggingVo loggingVo) {
        final String message = buildMessage(loggingVo);
        if (loggingVo.error()) {
            log.error(message);
        } else {
            log.info(message);
        }
    }

    @Override
    public void clearRequestResponse() {
    }

    private String buildMessage(final LoggingVo loggingVo) {
        return "headers = " + orDefault(loggingVo.requestHeaders()) + System.lineSeparator() +
                "method = " + orDefault(loggingVo.method()) + " " +
                "path = " + orDefault(loggingVo.requestURI()) + " " +
                "status = " + loggingVo.status() + " " +
                "duration = " + loggingVo.duration() + "ms " +
                "request = " + orDefault(loggingVo.requestBody()) + System.lineSeparator() +
                "response = " + orDefault(loggingVo.responseBody());
    }

    private String orDefault(final String value) {
        return StringUtils.hasText(value) ? value : EMPTY;
    }
}