package com.company.commonstarter.masking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingRulesTest {

    private final MaskingRules rules = new MaskingRules(MaskingRules.defaults());

    @Test
    void masksPasswordAndSecretJsonFields() {
        assertThat(rules.apply("{\"password\":\"hunter2\"}")).isEqualTo("{\"password\":\"***\"}");
        assertThat(rules.apply("{\"secret\":\"abc\"}")).isEqualTo("{\"secret\":\"***\"}");
        assertThat(rules.apply("{\"clientSecret\":\"abc\"}")).isEqualTo("{\"clientSecret\":\"***\"}");
    }

    @Test
    void masksBearerToken() {
        assertThat(rules.apply("Authorization: Bearer abc.def-ghi")).isEqualTo("Authorization: Bearer ***");
    }

    @Test
    void masksPrivateKey() {
        assertThat(rules.apply("private-key: MIIEvQIBADANBg")).isEqualTo("private-key: ***");
    }

    @Test
    void masksApiKeyAndApiAuthorizationHeaders() {
        assertThat(rules.apply("X-API-Key: abc123")).isEqualTo("X-API-Key: ---REDACTED---");
        assertThat(rules.apply("X-Api-Authorization: abc123")).isEqualTo("X-Api-Authorization: ---REDACTED---");
    }

    @Test
    void leavesCleanTextUntouched() {
        assertThat(rules.apply("nothing sensitive here")).isEqualTo("nothing sensitive here");
    }
}