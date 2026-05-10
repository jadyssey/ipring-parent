package org.ipring.jacg.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class JavaParseLogUtils {

    private static final String TIME_PATTERN = "HH:mm:ss";

    private JavaParseLogUtils() {
    }

    public static void logInfo(String message) {
        System.out.println(format("INFO", message));
    }

    public static void logWarn(String message) {
        System.out.println(format("WARN", message));
    }

    private static String format(String level, String message) {
        return "[" + new SimpleDateFormat(TIME_PATTERN).format(new Date()) + "] [" + level + "] " + message;
    }
}
