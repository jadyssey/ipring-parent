package org.ipring.model.param.order;

import org.ipring.anno.EnumValue;
import org.ipring.enums.order.ExpirationTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/8
 **/

@Data
public class OrderUpdateParam {
    @ApiModelProperty("交易账号")
    @NotNull(message = "{common.cant_null}")
    private Long accountId;

    @ApiModelProperty("订单号")
    @NotNull(message = "{common.cant_null}")
    private Long orderId;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeProfit;

    @ApiModelProperty("止损价格")
    private BigDecimal stopLoss;

    @ApiModelProperty("挂单价格 (买入/卖出价格)")
    private BigDecimal pendingPrice;

    @ApiModelProperty("订单过期模式")
    @EnumValue(type = ExpirationTypeEnum.class)
    private Integer expirationType;

    @ApiModelProperty("订单过期时间（13位时间戳）")
    private Long expiration;
}
