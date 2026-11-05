package com.company.commonstarter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.company.commonstarter.config.LoggingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleJsonMdcLoggingServiceTest {

    private final LoggingProperties properties = new LoggingProperties();
    private final ConsoleJsonMdcLoggingService service = new ConsoleJsonMdcLoggingService(properties);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        final Logger logger = (Logger) LoggerFactory.getLogger(ConsoleJsonMdcLoggingService.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
        ((Logger) LoggerFactory.getLogger(ConsoleJsonMdcLoggingService.class)).detachAppender(appender);
    }

    @Test
    void emitsExactlyOneLogLineCarryingAllHttpFieldsInMdc() {
        properties.setEnv("test");
        properties.setAppName("my-app");

        final LoggingVo vo = LoggingVo.builder()
                .method("GET").requestURI("/foo").status(200).duration(12)
                .requestBody("{}").responseBody("{\"ok\":true}").build();

        service.logRequestResponse(vo);

        assertThat(appender.list).hasSize(1);
        final ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getMDCPropertyMap())
                .containsEntry("env", "test")
                .containsEntry("app_name", "my-app")
                .containsEntry("http_method", "GET")
                .containsEntry("http_path", "/foo")
                .containsEntry("http_status", "200")
                .containsEntry("duration_ms", "12")
                .containsEntry("response", "{\"ok\":true}");
    }

    @Test
    void logsAtErrorLevelWhenMarkedAsError() {
        final LoggingVo vo = LoggingVo.builder()
                .method("GET").requestURI("/foo").status(500).duration(5).error(true).build();

        service.logRequestResponse(vo);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void clearsMdcAfterLogging() {
        properties.setEnv("test");
        properties.setAppName("my-app");

        final LoggingVo vo = LoggingVo.builder()
                .method("GET").requestURI("/foo").status(200).duration(12).build();

        service.logRequestResponse(vo);

        assertThat(MDC.get("http_method")).isNull();
        assertThat(MDC.get("env")).isNull();
        assertThat(MDC.get("app_name")).isNull();
    }
}