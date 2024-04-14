package org.ipring.model.vo.order;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.ipring.anno.EnumValue;
import org.ipring.enums.order.OrderStatusEnum;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.entity.order.TradeOrderEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/9
 **/

@Data
@ApiModel("下单成功返回模型")
public class MakeOrderVO {
    @ApiModelProperty("交易账号")
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("订单号")
    private Long orderId;

    @ApiModelProperty("市场代码")
    private Integer marketType;

    @ApiModelProperty("品种代码")
    private String symbolId;

    @EnumValue(type = OrderTypeEnum.class)
    @ApiModelProperty("订单类型")
    private Integer orderType;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("开仓时间")
    private Long openTime;

    @ApiModelProperty("真正的开仓价格")
    private BigDecimal realOpenPrice;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeProfit;

    @ApiModelProperty("止损价格")
    private BigDecimal stopLoss;

    @ApiModelProperty("挂单价格 (买入/卖出价格)")
    private BigDecimal pendingPrice;

    @ApiModelProperty("备注")
    private String comment;

    @EnumValue(type = OrderStatusEnum.class)
    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    public static MakeOrderVO of(TradeOrderEntity entity) {
        MakeOrderVO orderVO = new MakeOrderVO();
        BeanUtils.copyProperties(entity, orderVO);
        return orderVO;
    }
}
