package org.ipring.jacg.util;

public final class JavaParseTextUtils {

    private JavaParseTextUtils() {
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String normalizeInlineWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    public static String shortClassName(String fullName) {
        if (fullName == null) {
            return "";
        }
        int idx = fullName.lastIndexOf('.');
        return idx < 0 ? fullName : fullName.substring(idx + 1);
    }

    public static String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String type = normalizeInlineWhitespace(typeName);
        if (type.isEmpty()) {
            return null;
        }
        int genericPos = type.indexOf('<');
        if (genericPos >= 0) {
            type = type.substring(0, genericPos).trim();
        }
        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2).trim();
        }
        return type;
    }

    public static String normalizeScopeVar(String scopeExpr) {
        String scope = normalizeInlineWhitespace(scopeExpr);
        if (scope.startsWith("this.")) {
            return scope.substring("this.".length());
        }
        if (scope.startsWith("super.")) {
            return scope.substring("super.".length());
        }
        return scope;
    }

    public static boolean isTypeLikeScope(String scopeExpr) {
        String value = normalizeInlineWhitespace(scopeExpr);
        if (value.isEmpty()) {
            return false;
        }
        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            return false;
        }
        return Character.isUpperCase(value.charAt(0)) || value.contains(".");
    }

    public static String normalizePathSlash(String value) {
        return value == null ? "" : value.replace('\\', '/');
    }
}
