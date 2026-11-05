package com.company.commonstarter.mdc;

import com.company.commonstarter.correlation.CorrelationIdContext;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

@UtilityClass
public class MdcUtils {

    // mdcKey is passed in rather than fixed, since it's configurable per-consumer
    public static String getCorrelationId(final String mdcKey) {
        final String scopedCorrelationId = CorrelationIdContext.getCorrelationId();
        if (StringUtils.hasText(scopedCorrelationId)) {
            return scopedCorrelationId;
        }
        return MDC.get(mdcKey);
    }

    public static void putCorrelationId(final String mdcKey, final String correlationId) {
        MDC.put(mdcKey, correlationId);
    }

    public static void removeCorrelationId(final String mdcKey) {
        MDC.remove(mdcKey);
    }

    public static void putEnv(final String env) {
        MDC.put(MdcKeys.ENV, env);
    }

    public static void putAppName(final String appName) {
        MDC.put(MdcKeys.APP_NAME, appName);
    }

    public static void removeEnvAndAppName() {
        MDC.remove(MdcKeys.ENV);
        MDC.remove(MdcKeys.APP_NAME);
    }

    public static void putHttpMethod(final String method) {
        MDC.put(MdcKeys.HTTP_METHOD, method);
    }

    public static void putHttpPath(final String path) {
        MDC.put(MdcKeys.HTTP_PATH, path);
    }

    public static void putHttpStatus(final int status) {
        MDC.put(MdcKeys.HTTP_STATUS, String.valueOf(status));
    }

    public static void putDurationMs(final long duration) {
        MDC.put(MdcKeys.DURATION_MS, String.valueOf(duration));
    }

    public static void putRequestHeaders(final String headers) {
        MDC.put(MdcKeys.REQUEST_HEADERS, headers);
    }

    public static void putRequestBody(final String body) {
        MDC.put(MdcKeys.REQUEST_BODY, body);
    }

    public static void putResponseBody(final String body) {
        MDC.put(MdcKeys.RESPONSE_BODY, body);
    }

    public static void removeHttpFields() {
        MDC.remove(MdcKeys.HTTP_METHOD);
        MDC.remove(MdcKeys.HTTP_PATH);
        MDC.remove(MdcKeys.HTTP_STATUS);
        MDC.remove(MdcKeys.DURATION_MS);
        MDC.remove(MdcKeys.REQUEST_HEADERS);
        MDC.remove(MdcKeys.REQUEST_BODY);
        MDC.remove(MdcKeys.RESPONSE_BODY);
    }
}