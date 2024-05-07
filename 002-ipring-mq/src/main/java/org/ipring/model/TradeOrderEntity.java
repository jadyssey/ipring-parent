package org.ipring.model;

import org.springframework.beans.BeanUtils;

import java.util.Objects;

/**
 * @author lgj
 * @date 2024/4/8
 **/
public class TradeOrderEntity extends TradeOrderDO {

    public static TradeOrderEntity of(TradeOrderDO order) {
        if (Objects.isNull(order)) return null;
        TradeOrderEntity entity = new TradeOrderEntity();
        BeanUtils.copyProperties(order, entity);
        return entity;
    }
}
