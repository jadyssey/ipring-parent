package org.ipring.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author: Rainful
 * @date: 2024/04/02 15:21
 * @description:
 */
public abstract class DateUtil {
    public static final String TRADER_SESSION_TIME = "HHmm";

    public static final DateTimeFormatter TRADER_SESSION_TIME_FORMATTER = DateTimeFormatter.ofPattern(TRADER_SESSION_TIME);

    public static long getMills() {
        return Instant.now().toEpochMilli();
    }

    public static int getTraderSessionTime(LocalDateTime dateTime) {
        return Integer.parseInt(dateTime.format(TRADER_SESSION_TIME_FORMATTER));
    }
}
