package org.ipring.model.httpclient.response.ct4;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ipring.anno.EnumValue;
import org.ipring.enums.order.OrderStatusEnum;

import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/9
 **/

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("订单变更成功返回模型")
public class ModifyOrderVO extends CommandLogVO {
    @ApiModelProperty("交易账号")
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("订单号")
    private Long orderId;

    @ApiModelProperty("市场代码")
    private Integer marketType;

    @ApiModelProperty("品种代码")
    private String symbolId;

    @ApiModelProperty("订单类型")
    private Integer orderType;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("开仓时间")
    private Long openTime;

    @ApiModelProperty("真正的开仓价格（挂单的请求价格或挂单成交时市价）")
    private BigDecimal realOpenPrice;

    @ApiModelProperty("真实平仓价格")
    private BigDecimal realClosePrice;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeProfit;

    @ApiModelProperty("止损价格")
    private BigDecimal stopLoss;

    @ApiModelProperty("备注")
    private String comment;

    @EnumValue(type = OrderStatusEnum.class)
    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    @ApiModelProperty(value = "创建时间", example = "1713510524875")
    private Long createTime;

    @ApiModelProperty("税金")
    private BigDecimal texes = BigDecimal.ZERO; // 后面考虑怎么处理 应该从哪拿?

    @ApiModelProperty("报价的小数点位数")
    private Integer digits;

    @ApiModelProperty("隔夜利息")
    private BigDecimal swap;

    @ApiModelProperty("手续费")
    private BigDecimal commissions;

    @ApiModelProperty(value = "订单保证金")
    private BigDecimal margin;

    @ApiModelProperty("账号货币盈亏")
    private BigDecimal profit;
}
