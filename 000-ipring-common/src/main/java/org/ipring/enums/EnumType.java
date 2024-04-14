package org.ipring.enums;

/**
 * @author lgj
 * @date 14/2/2023
 **/
public interface EnumType<T> {

    /**
     * 获取枚举对应的值
     *
     * @return 枚举对应值
     */
    T getType();

    /**
     * 获取枚举对应的描述
     *
     * @return 枚举对应描述
     */
    String getDescription();
}
