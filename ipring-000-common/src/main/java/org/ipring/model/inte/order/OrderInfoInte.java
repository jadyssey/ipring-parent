package org.ipring.model.inte.order;

import java.math.BigDecimal;

/**
 * @author: lgj
 * @date: 2024/04/13 10:17
 * @description:
 */
public interface OrderInfoInte {

    Integer getOrderType();

    BigDecimal getSwap();

    BigDecimal getCommissions();

    BigDecimal getRealOpenPrice();

    Double getTicket();
}
