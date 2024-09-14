package org.ipring.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author: Rainful
 * @date: 2024/04/02 15:21
 * @description:
 */
public abstract class DateUtil {
    public static final String TRADER_SESSION_TIME = "HHmm";
    public static final String DAY_TIME = "yyyyMMddHHmmss";

    public static final DateTimeFormatter TRADER_SESSION_TIME_FORMATTER = DateTimeFormatter.ofPattern(TRADER_SESSION_TIME);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DAY_TIME);

    public static final DateTimeFormatter MM_DD_YYYY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter FORMAT_HH_mm_ss = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter DATE_14_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static long getMills() {
        return System.currentTimeMillis();
    }

    public static String getDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    public static String formatTimestamp(Long timestamp) {
        return DATE_14_FORMAT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
    }

    public static String getNowTimeByZone(ZoneOffset zoneOffset) {
        return LocalDateTime.now(zoneOffset).format(DATE_14_FORMAT);
    }

    public static void main(String[] args) {
        String nowTimeByZone = getNowTimeByZone(ZoneOffset.ofHours(0));
        System.out.println("nowTimeByZone = " + nowTimeByZone);

        String format = LocalDateTime.now(ZoneOffset.ofHours(0)).format(DATE_14_FORMAT);
        System.out.println("format = " + format);
    }
}
