package com.gravitee.migration.converter.factory.registry;

import com.gravitee.migration.converter.factory.PolicyConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PolicyConverterRegistry {
    private final List<PolicyConverter> converters;

    public PolicyConverter getConverter(String policyType) {
        return converters.stream()
                .filter(converter -> converter.supports(policyType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No converter found for policy type: " + policyType));
    }
}
