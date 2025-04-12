package com.gravitee.migration.enums;

import lombok.Getter;

@Getter
public enum SignatureEnum {
    RS256("RSA_RS256"),
    RS384("RSA_RS384"),
    RS512("RSA_RS512");

    private final String value;

    SignatureEnum(String value) {
        this.value = value;
    }

    public static String fromValue(String value) {
        return switch (value) {
            case "RS256" -> RS256.getValue();
            case "RS384" -> RS384.getValue();
            case "RS512" -> RS512.getValue();
            default -> throw new IllegalArgumentException("Unknown value: " + value);
        };
    }

}
