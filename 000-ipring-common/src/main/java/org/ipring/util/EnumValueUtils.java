package org.ipring.util;

import org.ipring.anno.EnumValue;
import org.ipring.enums.EnumType;
import org.ipring.enums.IntEnumType;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author lgj
 * @date 8/2/2023
 **/

@Slf4j
public class EnumValueUtils {

    /**
     * 拼接枚举值和描述
     *
     * @param enumValue 含有枚举对象的注解
     * @param value     枚举值描述
     * @return
     */
    public static String appendEnumValue(EnumValue enumValue, String value) {
        return appendEnumValue(enumValue.type(), value);
    }


    /**
     * 拼接枚举值和描述
     *
     * @param cls   枚举对象
     * @param value 枚举对象描述
     * @return
     */
    public static String appendEnumValue(Class<? extends EnumType<?>> cls, String value) {
        StringBuilder description = new StringBuilder(value);
        description.append("（");
        EnumType<?>[] baseEnums = cls.getEnumConstants();
        for (EnumType<?> baseEnum : baseEnums) {
            description.append("<font color='#FF0000'>").append(baseEnum.getType()).append("</font>").append("：").append(baseEnum.getDescription()).append("，");
        }
        description.replace(description.length() - 1, description.length(), "）");
        return description.toString();
    }

    public static String getEnumAllowableValues(Class<? extends EnumType> enumClass) {
        EnumType[] baseEnums = enumClass.getEnumConstants();
        StringBuilder allowableValues = new StringBuilder();
        for (EnumType baseEnum : baseEnums) {
            allowableValues.append(",").append(baseEnum.getType());
        }
        return allowableValues.toString().replaceFirst(",", "");
    }

    /**
     * 判断枚举类中是否包含有某个值
     *
     * @param enumClazz 枚举类
     * @param value     要查找的值
     * @return
     */
    public static <T> boolean contains(Class<? extends EnumType<?>> enumClazz, T value) {
        EnumType<?>[] baseEnums = enumClazz.getEnumConstants();

        if (value instanceof Integer) {
            for (EnumType<?> baseEnum : baseEnums) {
                if (Objects.equals(baseEnum.getType(), value)) {
                    return true;
                }
            }
        } else if (value instanceof String) {
            for (EnumType<?> baseEnum : baseEnums) {
                if (baseEnum.getType().toString().equalsIgnoreCase((String) value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过枚举值获取描述信息
     *
     * @param enumClazz
     * @param value
     * @param <E>
     * @return
     */
    public static <E extends IntEnumType> String getDescByValue(Class<E> enumClazz, Integer value) {
        return getMapping(enumClazz).get(value);
    }

    /**
     * 获取枚举值和描述映射关系
     *
     * @param enumClazz 枚举类clazz对象
     * @return map集合，key:枚举值，value:描述信息
     */
    public static <E extends IntEnumType> Map<Integer, String> getMapping(Class<E> enumClazz) {
        IntEnumType[] baseEnums = enumClazz.getEnumConstants();

        Map<Integer, String> mapping = new LinkedHashMap<>(baseEnums.length);
        for (IntEnumType baseEnum : baseEnums) {
            mapping.put(baseEnum.getType(), baseEnum.getDescription());
        }
        return mapping;
    }
}
