package com.company.commonstarter.masking;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaskingConverterTest {

    private final MaskingConverter converter = new MaskingConverter();

    @Test
    void masksSensitiveDataInFormattedMessage() {
        final ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("login with {\"password\":\"hunter2\"} Bearer abc123");

        final String result = converter.convert(event);

        assertThat(result).doesNotContain("hunter2", "abc123");
        assertThat(result).contains("\"password\":\"***\"", "Bearer ***");
    }
}