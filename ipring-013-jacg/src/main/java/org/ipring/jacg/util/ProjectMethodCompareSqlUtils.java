package org.ipring.jacg.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 复用 projectMethodCompare 目录中的 SQL 文本格式化辅助逻辑。
 */
public final class ProjectMethodCompareSqlUtils {

    private ProjectMethodCompareSqlUtils() {
    }

    /**
     * 统一 Mapper SQL 的换行风格。
     */
    public static String normalizeMapperSql(String rawSql) {
        if (rawSql == null) {
            return "";
        }
        return rawSql.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * 将 SQL 文本拆成多行并清理首尾空白行。
     */
    public static List<String> splitSqlLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = normalizeMapperSql(text);
        String[] rawLines = normalized.split("\n", -1);

        int start = 0;
        while (start < rawLines.length && rawLines[start].trim().isEmpty()) {
            start++;
        }

        int end = rawLines.length - 1;
        while (end >= start && rawLines[end].trim().isEmpty()) {
            end--;
        }

        if (start > end) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            lines.add(rtrim(rawLines[i].replace("\t", "    ")));
        }
        return lines;
    }

    /**
     * 统计一行文本前导空白的宽度。
     */
    public static int countLeadingWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t') {
                break;
            }
            i++;
        }
        return i;
    }

    /**
     * 最多移除指定数量的前导空白，保持 SQL 对齐。
     */
    public static String stripLeadingWhitespace(String text, int maxCount) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (maxCount <= 0) {
            return text;
        }

        int i = 0;
        while (i < text.length() && i < maxCount) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t') {
                break;
            }
            i++;
        }
        return text.substring(i);
    }

    /**
     * 去掉行尾空白，避免输出里出现多余空格。
     */
    public static String rtrim(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
