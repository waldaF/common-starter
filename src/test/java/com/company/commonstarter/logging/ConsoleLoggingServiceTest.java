package com.company.commonstarter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleLoggingServiceTest {

    private final ConsoleLoggingService service = new ConsoleLoggingService();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ConsoleLoggingService.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(ConsoleLoggingService.class)).detachAppender(appender);
    }

    @Test
    void buildsSingleHumanReadableMessageWithAllFields() {
        final LoggingVo vo = LoggingVo.builder()
                .method("POST").requestURI("/foo").status(201).duration(42)
                .requestHeaders("Accept: */*;").requestBody("{\"a\":1}").responseBody("{\"id\":9}")
                .build();

        service.logRequestResponse(vo);

        assertThat(appender.list).hasSize(1);
        final String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("method = POST", "path = /foo", "status = 201", "duration = 42ms",
                "request = {\"a\":1}", "response = {\"id\":9}", "headers = Accept: */*;");
    }

    @Test
    void defaultsMissingFieldsToEmptyBraces() {
        final LoggingVo vo = LoggingVo.builder().method("GET").requestURI("/foo").status(200).duration(1).build();

        service.logRequestResponse(vo);

        assertThat(appender.list.get(0).getFormattedMessage()).contains("request = {}", "response = {}");
    }

    @Test
    void logsAtErrorLevelWhenMarkedAsError() {
        final LoggingVo vo = LoggingVo.builder().method("GET").requestURI("/foo").status(500).error(true).build();

        service.logRequestResponse(vo);

        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }
}