package com.company.commonstarter.autoconfigure;

import com.company.commonstarter.config.MaskingProperties;
import com.company.commonstarter.masking.MaskingRule;
import com.company.commonstarter.masking.MaskingRules;
import com.company.commonstarter.masking.MaskingRulesTextUpdater;
import com.company.commonstarter.masking.TextUpdater;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@AutoConfiguration
@ConditionalOnProperty(prefix = "company.masking", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MaskingProperties.class)
public class MaskingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MaskingRules maskingRules(final MaskingProperties properties) {
        final List<MaskingRule> rules = new ArrayList<>(MaskingRules.defaults());
        for (final MaskingProperties.Rule rule : properties.getRules()) {
            rules.add(new MaskingRule(Pattern.compile(rule.pattern()), rule.replacement()));
        }
        return new MaskingRules(rules);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "company.masking", name = "apply-to-http-logging", matchIfMissing = true)
    public TextUpdater maskingTextUpdater(final MaskingRules maskingRules) {
        return new MaskingRulesTextUpdater(maskingRules);
    }
}