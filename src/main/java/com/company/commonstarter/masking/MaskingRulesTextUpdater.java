package com.company.commonstarter.masking;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaskingRulesTextUpdater implements TextUpdater {

    private final MaskingRules maskingRules;

    @Override
    public String update(final String text) {
        return maskingRules.apply(text);
    }
}