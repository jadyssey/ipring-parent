package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author: Rainful
 * @date: 2024/04/08 19:22
 * @description:
 */
public interface OrderOpenAndCloseEnum {

    @RequiredArgsConstructor
    @Getter
    enum OrderTimingEnum implements IntEnumType {

        OPEN(1, "开仓"),
        CLOSE(2, "平仓"),
        OPEN_CLOSE(3, "开仓&平仓"),
        ;
        private final Integer type;
        private final String description;
    }

    @RequiredArgsConstructor
    @Getter
    enum OpenTypeEnum implements IntEnumType {

        AUTO(1, "自动开仓"),
        MANUAL(2, "手动开仓"),
        ;
        private final Integer type;
        private final String description;
    }

    @RequiredArgsConstructor
    @Getter
    enum CloseTypeEnum implements IntEnumType {

        AUTO(1, "自动平仓"),
        MANUAL(2, "手动平仓"),
        FORCED_CLOSE(3, "强制平仓"),
        ;
        private final Integer type;
        private final String description;
    }
}
