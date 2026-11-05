package com.company.commonstarter.masking;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingConverter extends MessageConverter {

    private static final MaskingRules RULES = new MaskingRules(MaskingRules.defaults());

    @Override
    public String convert(final ILoggingEvent event) {
        return RULES.apply(super.convert(event));
    }
}