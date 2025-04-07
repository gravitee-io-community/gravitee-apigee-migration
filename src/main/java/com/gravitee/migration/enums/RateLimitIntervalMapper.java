package com.gravitee.migration.enums;

public class RateLimitIntervalMapper {

    private RateLimitIntervalMapper() {
    }

    public static String map(String interval) {
        return switch (interval) {
            case "second" -> "SECONDS";
            case "minute" -> "MINUTES";
            case "hour" -> "HOURS";
            case "day" -> "DAYS";
            default -> interval;
        };
    }

    public static String mapRate(String interval) {
        return switch (interval) {
            case "ps" -> "SECONDS";
            default -> interval;
        };
    }


    public static int mapRateToInt(String interval) {
        return switch (interval) {
            case "ps" -> 1;
            default -> throw new RuntimeException();
        };
    }
}
