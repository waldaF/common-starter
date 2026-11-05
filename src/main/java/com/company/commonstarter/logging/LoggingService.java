package com.company.commonstarter.logging;

public interface LoggingService {

    void logRequestResponse(LoggingVo loggingVo);

    void clearRequestResponse();
}