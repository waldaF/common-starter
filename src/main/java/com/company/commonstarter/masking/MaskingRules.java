package com.company.commonstarter.masking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public class MaskingRules {

    private final List<MaskingRule> rules;

    public static List<MaskingRule> defaults() {
        return List.of(
                new MaskingRule(Pattern.compile("(?i)(\"(?:password|secret|clientSecret)\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*(\")"), "$1***$2"),
                new MaskingRule(Pattern.compile("(?i)(Bearer\\s+)[\\w\\-.+/]+=*"), "$1***"),
                new MaskingRule(Pattern.compile("(?i)(private[-_]?key[^:]*:\\s*)[^\\n]+"), "$1***"),
                new MaskingRule(Pattern.compile("(?i)(X-API-Key:\\s*)[^\\s]+"), "$1---REDACTED---"),
                new MaskingRule(Pattern.compile("(?i)(X-Api-Authorization:\\s*)[^\\s]+"), "$1---REDACTED---"));
    }

    public String apply(final String text) {
        var result = text;
        for (final MaskingRule rule : rules) {
            result = rule.apply(result);
        }
        return result;
    }
}