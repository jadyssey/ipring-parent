package org.ipring.util;

import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.entity.order.SymbolMsgDTO;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/10 17:31
 * @description:
 */
public abstract class SymbolMsgUtil {

    /**
     * 开单的时候 获取买价
     *
     * @param symbolMsg 实时报价模型
     */
    public static BigDecimal openPrice(SymbolMsgDTO symbolMsg, Integer operation) {
        return OrderTypeEnum.ALL_ENUM_MAP.get(operation).getOpenPrice().apply(symbolMsg);
    }

    public static BigDecimal closePrice(SymbolMsgDTO symbolMsg, Integer operation) {
        return OrderTypeEnum.ALL_ENUM_MAP.get(operation).getClosePrice().apply(symbolMsg);
    }
}
