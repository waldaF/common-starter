package com.company.commonstarter.logging;

import com.company.commonstarter.config.LoggingProperties;
import com.company.commonstarter.mdc.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Pushes every HTTP field into MDC, then makes exactly one log call -- a JSON-structured
// logback encoder flattens MDC into a single structured log line with everything in it.
@RequiredArgsConstructor
@Slf4j
public class ConsoleJsonMdcLoggingService implements LoggingService {

    private final LoggingProperties properties;

    @Override
    public void logRequestResponse(final LoggingVo loggingVo) {
        MdcUtils.putEnv(properties.getEnv());
        MdcUtils.putAppName(properties.getAppName());
        MdcUtils.putHttpMethod(loggingVo.method());
        MdcUtils.putHttpPath(loggingVo.requestURI());
        MdcUtils.putHttpStatus(loggingVo.status());
        MdcUtils.putDurationMs(loggingVo.duration());
        MdcUtils.putRequestHeaders(loggingVo.requestHeaders());
        MdcUtils.putRequestBody(loggingVo.requestBody());
        MdcUtils.putResponseBody(loggingVo.responseBody());

        final String baseMessage = buildBaseMessage(loggingVo);
        if (loggingVo.error()) {
            log.error(baseMessage);
        } else {
            log.info(baseMessage);
        }
        clearRequestResponse();
    }

    @Override
    public void clearRequestResponse() {
        MdcUtils.removeHttpFields();
        MdcUtils.removeEnvAndAppName();
    }

    private String buildBaseMessage(final LoggingVo loggingVo) {
        return loggingVo.method() + " " + loggingVo.requestURI()
                + " -> " + loggingVo.status() + " (" + loggingVo.duration() + "ms)";
    }
}