package com.company.commonstarter.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

@UtilityClass
public class HeaderUtils {

    public static String extractHeadersFromRequest(final HttpServletRequest request) {
        final StringBuilder headers = new StringBuilder();
        final Enumeration<String> names = request.getHeaderNames();
        for (final String name : Collections.list(Objects.nonNull(names) ? names : Collections.emptyEnumeration())) {
            headers.append(name).append(": ").append(request.getHeader(name)).append("; ");
        }
        return headers.toString();
    }
}