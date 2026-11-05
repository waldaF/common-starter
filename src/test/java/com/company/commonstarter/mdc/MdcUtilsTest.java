package com.company.commonstarter.mdc;

import com.company.commonstarter.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcUtilsTest {

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void prefersScopedValueOverMdc() {
        MDC.put("correlationId", "from-mdc");

        CorrelationIdContext.runWithCorrelationId("from-scoped-value",
                () -> assertThat(MdcUtils.getCorrelationId("correlationId")).isEqualTo("from-scoped-value"));
    }

    @Test
    void fallsBackToMdcOutsideScopedValue() {
        MDC.put("correlationId", "from-mdc");

        assertThat(MdcUtils.getCorrelationId("correlationId")).isEqualTo("from-mdc");
    }

    @Test
    void returnsNullWhenNeitherIsSet() {
        assertThat(MdcUtils.getCorrelationId("correlationId")).isNull();
    }
}