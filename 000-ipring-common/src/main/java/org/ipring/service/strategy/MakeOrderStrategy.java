package org.ipring.service.strategy;

import org.ipring.model.entity.order.OrderStrategyDTO;
import org.ipring.model.entity.order.TradeOrderEntity;

/**
 * @author lgj
 * @date 2024/4/3
 **/
public interface MakeOrderStrategy {
    /**
     * 处理订单
     *
     * @param param
     */
    TradeOrderEntity order(OrderStrategyDTO param);
}