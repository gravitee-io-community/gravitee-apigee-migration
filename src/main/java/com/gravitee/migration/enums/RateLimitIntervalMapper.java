package com.gravitee.migration.enums;

public class RateLimitIntervalMapper {

    private RateLimitIntervalMapper() {
    }

    /**
     * Maps a shorthand rate interval to its full unit.
     *
     * @param interval the shorthand interval (e.g., "ps").
     * @return the mapped time unit (e.g., "SECONDS").
     */
    public static String mapShorthandRate(String interval) {
        if (interval == null || interval.isEmpty()) {
            throw new IllegalArgumentException("Interval cannot be null or empty");
        }
        return "ps".equals(interval) ? "SECONDS" : interval;
    }

    /**
     * Maps a shorthand rate interval to its numeric equivalent.
     *
     * @param interval the shorthand interval (e.g., "ps").
     * @return the numeric equivalent of the interval (e.g., 1 for "ps").
     * @throws IllegalArgumentException if the interval is not recognized.
     */
    public static int mapShorthandRateToInt(String interval) {
        if (interval == null || interval.isEmpty()) {
            throw new IllegalArgumentException("Interval cannot be null or empty");
        }
        // per second
        if ("ps".equals(interval)) {
            return 1;
        }
        throw new IllegalArgumentException("Unsupported interval: " + interval);
    }
}
