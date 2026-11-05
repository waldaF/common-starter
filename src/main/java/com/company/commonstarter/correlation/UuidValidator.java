package com.company.commonstarter.correlation;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@UtilityClass
public class UuidValidator {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public static boolean isValidUuid(final String value) {
        return StringUtils.hasText(value) && UUID_PATTERN.matcher(value).matches();
    }
}