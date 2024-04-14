package org.ipring.model.entity.order;

import org.ipring.model.dobj.trade.TradeOrderDO;
import org.ipring.model.inte.order.OrderInfoInte;
import org.springframework.beans.BeanUtils;

/**
 * @author lgj
 * @date 2024/4/8
 **/
public class TradeOrderEntity extends TradeOrderDO implements OrderInfoInte {

    public static TradeOrderEntity of(TradeOrderDO order) {
        TradeOrderEntity entity = new TradeOrderEntity();
        BeanUtils.copyProperties(order, entity);
        return entity;
    }
}
