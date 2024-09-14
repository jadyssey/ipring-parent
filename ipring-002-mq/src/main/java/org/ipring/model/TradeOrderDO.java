package org.ipring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ipring.anno.EnumValue;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.enums.order.ExpirationTypeEnum;
import org.ipring.enums.order.OrderStatusEnum;

import java.math.BigDecimal;

/**
 * @author lgj
 * @description trade_order
 * @date 2024-04-03
 */
@Data
public class TradeOrderDO extends OrderUniqEntity {

    private Long id;

    @ApiModelProperty(value = "账户id", example = "123")
    private Long accountId;

    @ApiModelProperty("成交单号(暂未使用)")
    private Long exchorderId;

    @ApiModelProperty("撮合单号(暂未使用)")
    private Long matchId;

    @ApiModelProperty("市场代码")
    private Integer marketType;

    @ApiModelProperty("品种代码")
    private String symbolId;

    @ApiModelProperty("订单类型")
    private Integer orderType;

    @Deprecated
    @ApiModelProperty("0：buy， 1: sell")
    private Integer buyOrSell;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("下挂单请求的价格 (用户的请求下单的价格，仅用作记录，不给客户端)")
    private BigDecimal pendingPrice;

    @ApiModelProperty("开仓价格/ 挂单则为当时市价（用于记录）")
    private BigDecimal openPrice;

    @ApiModelProperty("真正的开仓价格（挂单的请求价格或挂单成交时市价）")
    private BigDecimal realOpenPrice;

    @ApiModelProperty("开仓时间")
    private Long openTime;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeProfit;

    @ApiModelProperty("止损价格")
    private BigDecimal stopLoss;

    @ApiModelProperty("账号货币盈亏")
    private BigDecimal profit;

    @ApiModelProperty("盈亏货币盈亏")
    private BigDecimal profitAsProfitCurrency;

    @ApiModelProperty("收益率")
    private BigDecimal profitRate;

    @ApiModelProperty("点数")
    private Long point;

    @ApiModelProperty("隔夜利息")
    private BigDecimal swap;

    @ApiModelProperty("手续费")
    private BigDecimal commissions;

    @ApiModelProperty(value = "订单保证金")
    private BigDecimal margin;

    @ApiModelProperty("用户备注")
    private String comment;

    @ApiModelProperty("系统备注")
    private String remark;

    @ApiModelProperty("订单过期模式")
    @EnumValue(type = ExpirationTypeEnum.class)
    private Integer expirationType;

    @ApiModelProperty("订单过期时间")
    private Long expiration;

    @ApiModelProperty("移动止损点数")
    private Integer dynamicSlPoint;

    @ApiModelProperty("移动止损价格")
    private BigDecimal dynamciSlPirce;

    @ApiModelProperty("滑点误差（按点数给，无限制则不传或传空）")
    private Long slippage;

    @ApiModelProperty("平仓价格")
    private BigDecimal closePrice;

    @ApiModelProperty("真实平仓价格")
    private BigDecimal realClosePrice;

    @ApiModelProperty("平仓时间")
    private Long closeTime;

    @ApiModelProperty("订单触发原因")
    private Integer dealReason;

    @ApiModelProperty("开仓类型")
    private Integer openType;

    @ApiModelProperty("平仓类型")
    private Integer closeType;

    @ApiModelProperty("涨跌幅")
    private Long priceChangeRate;

    @ApiModelProperty("是否允许盘前盘后成交")
    @EnumValue(type = BoolTypeInt.class, nullable = false)
    private Integer overTrade;

    @EnumValue(type = OrderStatusEnum.class)
    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    private Long createTime;
    private String creator;
    private String modifier;
}