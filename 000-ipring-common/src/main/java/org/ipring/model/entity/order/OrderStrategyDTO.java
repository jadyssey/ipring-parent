package org.ipring.model.entity.order;

import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.param.order.OrderAddParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/3
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStrategyDTO {
    private TradeSymbolDO symbol;
    private OrderAddParam orderAddParam;

    @ApiModelProperty("订单号")
    private Long orderId;

    @ApiModelProperty("真实下单价格")
    private BigDecimal price;

    @ApiModelProperty("手续费")
    private BigDecimal commissions;

    @ApiModelProperty("所需保证金")
    private BigDecimal margin;
}
