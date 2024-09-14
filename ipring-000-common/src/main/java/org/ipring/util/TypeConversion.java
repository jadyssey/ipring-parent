package org.ipring.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;

import java.util.Collection;

/**
 * @author lgj
 * @date 2024/4/3
 **/
public class TypeConversion {
    public static <T> boolean isCollectionType(T t) {
        return t instanceof Collection;
    }

    /**
     * 简单字符串和基本类型统称为简单类型
     */
    public static <T> boolean isSimpleType(T t) {
        return isSimpleString(t) || isInt(t) || isLong(t) || isDouble(t) || isFloat(t) || isChar(t) || isBoolean(t) || isShort(t) || isByte(t);
    }

    public static <T> boolean isSimpleString(T t) {
        if (!isString(t)) {
            return false;
        }
        return !JSONUtil.isJson(t.toString());
    }

    public static <T> boolean isString(T t) {
        return t instanceof String;
    }

    public static <T> boolean isByte(T t) {
        return t instanceof Byte;
    }

    public static <T> boolean isShort(T t) {
        return t instanceof Short;
    }

    public static <T> boolean isInt(T t) {
        return t instanceof Integer;
    }

    public static <T> boolean isLong(T t) {
        return t instanceof Long;
    }

    public static <T> boolean isChar(T t) {
        return t instanceof Character;
    }

    public static <T> boolean isFloat(T t) {
        return t instanceof Float;
    }

    public static <T> boolean isDouble(T t) {
        return t instanceof Double;
    }

    public static <T> boolean isBoolean(T t) {
        return t instanceof Boolean;
    }

    public static <T> Class<?> getClassType(T t) {
        return t.getClass();
    }

    public static <R> R convertor(String str, Class<R> type) {
        return Convert.convert(type, str);
    }
}
