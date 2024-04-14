package org.ipring.enums;

/**
 * @author lgj
 * @date 8/2/2023
 **/
public interface IntEnumType extends EnumType<Integer> {

    /**
     * 判断该枚举的SubCode与参数SubCode是否相等
     *
     * @param code code值
     * @return 是否相等
     */
    default boolean equalTo(Integer code) {
        return this.getType().equals(code);
    }
}
