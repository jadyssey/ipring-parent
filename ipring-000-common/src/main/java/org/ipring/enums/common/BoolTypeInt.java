package org.ipring.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipring.enums.IntEnumType;

/**
 * @author lgj
 * @date 8/2/2023
 **/

@Getter
@AllArgsConstructor
public enum BoolTypeInt implements IntEnumType {
    // 其他
    NO(0, "否"),

    YES(1, "是"),
    ;

    public static boolean isTrue(Integer code) {
        return code != null && YES.equalTo(code);
    }
    /**
     * 判断是否为false
     *
     * @param value
     * @return
     */
    public static boolean isFalse(Integer value) {
        return value != null && NO.equalTo(value);
    }

    public static int toEnum(boolean flag)  {
        return flag ? YES.getType() : NO.getType();
    }

    private final Integer type;

    private final String description;
}
