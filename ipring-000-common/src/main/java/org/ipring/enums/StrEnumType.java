package org.ipring.enums;

/**
 * 默认大小写不敏感
 *
 * @author lgj
 * @date 8/2/2023
 **/
public interface StrEnumType extends EnumType<String> {

    /**
     * 判断该枚举的SubCode与参数SubCode是否相等
     * 大小写不敏感
     *
     * @param code code值
     * @return 是否相等
     */
    default boolean equalTo(String code) {
        return code != null && this.getType().equalsIgnoreCase(code);
    }
}
