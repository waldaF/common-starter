package com.company.commonstarter.masking;

import java.util.regex.Pattern;

public record MaskingRule(Pattern pattern, String replacement) {

    public String apply(final String text) {
        return pattern.matcher(text).replaceAll(replacement);
    }
}