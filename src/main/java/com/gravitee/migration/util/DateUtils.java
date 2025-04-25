package com.gravitee.migration.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {

    /**
     * Converts milliseconds since epoch to ISO 8601 format.
     *
     * @param millis the milliseconds since epoch
     * @return the ISO 8601 formatted date string
     */
    public static String convertMillisToIso8601(long millis) {
        OffsetDateTime dateTime = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return dateTime.format(formatter);
    }
}
