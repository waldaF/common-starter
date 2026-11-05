package com.company.commonstarter.mdc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MdcKeys {

    public static final String CORRELATION_ID = "correlation_id";
    public static final String ENV = "env";
    public static final String APP_NAME = "app_name";
    public static final String HTTP_METHOD = "http_method";
    public static final String HTTP_PATH = "http_path";
    public static final String HTTP_STATUS = "http_status";
    public static final String DURATION_MS = "duration_ms";
    public static final String REQUEST_HEADERS = "request_headers";
    public static final String REQUEST_BODY = "request";
    public static final String RESPONSE_BODY = "response";
}