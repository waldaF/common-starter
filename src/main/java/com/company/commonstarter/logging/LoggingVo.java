package com.company.commonstarter.logging;

import lombok.Builder;

@Builder
public record LoggingVo(String requestURI, String method, int status, long duration, String requestHeaders,
                        String requestBody, String responseBody, boolean error) {

}