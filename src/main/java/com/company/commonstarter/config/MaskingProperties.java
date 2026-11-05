package com.company.commonstarter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "company.masking")
public class MaskingProperties {

    private boolean enabled = true;
    private boolean applyToHttpLogging = true;
    private List<Rule> rules = List.of();

    public record Rule(String pattern, String replacement) {}
}