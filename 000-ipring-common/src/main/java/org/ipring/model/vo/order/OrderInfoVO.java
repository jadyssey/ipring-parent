package org.ipring.model.vo.order;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.ipring.anno.EnumValue;
import org.ipring.enums.order.OrderStatusEnum;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.dobj.trade.TradeOrderDO;
import org.ipring.model.inte.order.OrderInfoInte;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/8
 **/
@Data
@ApiModel("订单详情模型")
public class OrderInfoVO implements OrderInfoInte {
    @ApiModelProperty("交易账号")
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("订单号")
    private Long orderId;

    @ApiModelProperty("品种所在市场代码")
    private Integer marketType;

    @ApiModelProperty("品种代码")
    private String symbolId;

    @ApiModelProperty("订单类型")
    @EnumValue(type = OrderTypeEnum.class)
    private Integer orderType;

    @EnumValue(type = OrderStatusEnum.class)
    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("真正的开仓价格")
    private BigDecimal realOpenPrice;

    @ApiModelProperty("真实平仓价格")
    private BigDecimal realClosePrice;

    @ApiModelProperty("隔夜利息")
    private BigDecimal swap;

    @ApiModelProperty("手续费")
    private BigDecimal commissions;

    @ApiModelProperty("保证金")
    private BigDecimal margin;

    @ApiModelProperty("挂单价格 (买入/卖出价格)")
    private BigDecimal pendingPrice;

    @ApiModelProperty("账号货币盈亏")
    private BigDecimal profit;

    @ApiModelProperty("备注")
    private String comment;

    public static OrderInfoVO of(TradeOrderDO order) {
        OrderInfoVO resp = new OrderInfoVO();
        BeanUtils.copyProperties(order, resp);
        return resp;
    }
}
