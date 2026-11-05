package com.company.commonstarter.correlation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UuidValidatorTest {

    @Test
    void acceptsValidUuid() {
        assertThat(UuidValidator.isValidUuid("f47ac10b-58cc-4372-a567-0e02b2c3d479")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", "f47ac10b-58cc-4372-a567", "f47ac10b58cc4372a5670e02b2c3d479"})
    void rejectsInvalidValues(final String value) {
        assertThat(UuidValidator.isValidUuid(value)).isFalse();
    }
}