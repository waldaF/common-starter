package com.company.commonstarter.correlation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CorrelationIdContext {

    private static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    public static String getCorrelationId() {
        return CORRELATION_ID.isBound() ? CORRELATION_ID.get() : null;
    }

    public static void runWithCorrelationId(final String correlationId, final Runnable action) {
        ScopedValue.where(CORRELATION_ID, correlationId).run(action);
    }
}