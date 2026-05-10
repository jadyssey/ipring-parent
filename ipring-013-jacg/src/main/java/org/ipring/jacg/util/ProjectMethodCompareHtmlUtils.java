package org.ipring.jacg.util;

/**
 * 复用 projectMethodCompare 目录中的 HTML diff 渲染辅助逻辑。
 */
public final class ProjectMethodCompareHtmlUtils {

    private ProjectMethodCompareHtmlUtils() {
    }

    /**
     * 生成使用普通文本单元格的 diff 行。
     */
    public static String simpleRow(Integer leftNo, Integer rightNo, String leftText, String rightText, String cls) {
        return "<tr>"
                + simpleTd(leftNo, "ln")
                + simpleTd(escape(leftText), cls)
                + simpleTd(rightNo, "ln")
                + simpleTd(escape(rightText), cls)
                + "</tr>";
    }

    /**
     * 生成带缩进样式代码单元格的 diff 行。
     */
    public static String codeRow(Integer leftNo, Integer rightNo, String leftText, String rightText, String cls) {
        return "<tr>"
                + td(leftNo, "ln")
                + codeCell(leftText, cls)
                + td(rightNo, "ln")
                + codeCell(rightText, cls)
                + "</tr>";
    }

    /**
     * 转义 HTML 特殊字符，避免日志文本破坏页面结构。
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String codeCell(String text, String cls) {
        String normalized = text == null ? "" : text.replace("\t", "    ");
        int rawIndent = leadingSpaces(normalized);
        int indent = Math.min(rawIndent, 24);
        String content = normalized.substring(rawIndent);
        String classes = "code" + (cls == null || cls.trim().isEmpty() ? "" : " " + cls.trim());
        return "<td class='" + classes + "' style='--indent:" + indent + ";'><span class='line'>"
                + escape(content)
                + "</span></td>";
    }

    private static String td(Object value, String cls) {
        String classes = cls == null ? "" : cls.trim();
        return "<td" + (classes.isEmpty() ? "" : " class='" + classes + "'") + ">" + (value == null ? "" : value) + "</td>";
    }

    private static String simpleTd(Object value, String cls) {
        return "<td class='" + (cls == null ? "" : cls) + "'>" + (value == null ? "" : value) + "</td>";
    }

    private static int leadingSpaces(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int i = 0;
        while (i < text.length() && text.charAt(i) == ' ') {
            i++;
        }
        return i;
    }
}
