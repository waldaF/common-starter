package com.company.commonstarter.logging;

import lombok.experimental.UtilityClass;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@UtilityClass
public class PathMatchUtils {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static boolean matchesAny(final List<String> patterns, final String path) {
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}