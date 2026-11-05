package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.masking.MaskingRules;
import com.company.commonstarter.masking.TextUpdater;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MaskingAutoConfiguration.class));

    @Test
    void createsTextUpdaterByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(TextUpdater.class));
    }

    @Test
    void disabledByProperty() {
        contextRunner.withPropertyValues("company.masking.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MaskingRules.class));
    }

    @Test
    void textUpdaterSkippedWhenApplyToHttpLoggingDisabled() {
        contextRunner.withPropertyValues("company.masking.apply-to-http-logging=false")
                .run(context -> assertThat(context).doesNotHaveBean(TextUpdater.class));
    }

    @Test
    void customRulesAreMergedWithDefaults() {
        contextRunner.withPropertyValues(
                        "company.masking.rules[0].pattern=(?i)(ssn=)\\d+",
                        "company.masking.rules[0].replacement=$1***")
                .run(context -> {
                    final MaskingRules rules = context.getBean(MaskingRules.class);
                    assertThat(rules.getRules()).hasSize(MaskingRules.defaults().size() + 1);
                    assertThat(rules.apply("ssn=123456789")).isEqualTo("ssn=***");
                });
    }
}