package com.company.commonstarter.logging;

import com.company.commonstarter.masking.TextUpdater;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@UtilityClass
class LoggingTextSupport {

    private static final String EMPTY = "{}";

    static String limitAndSanitize(final byte[] content, final int maxChars, final List<TextUpdater> textUpdaters) {
        if (content.length == 0) {
            return EMPTY;
        }
        final String body = sanitize(new String(content, StandardCharsets.UTF_8), textUpdaters);
        if (body.length() > maxChars) {
            return body.substring(0, maxChars) + "...(truncated)";
        }
        return body;
    }

    static String sanitize(final String text, final List<TextUpdater> textUpdaters) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        var result = text;
        for (final TextUpdater updater : textUpdaters) {
            result = updater.update(result);
        }
        return result;
    }
}