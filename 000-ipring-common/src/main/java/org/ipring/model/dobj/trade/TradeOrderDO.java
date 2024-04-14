package org.ipring.model.dobj.trade;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.ipring.anno.EnumValue;
import org.ipring.enums.order.OrderStatusEnum;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.param.order.OrderAddParam;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author lgj
 * @description trade_order
 * @date 2024-04-03
 */
@Data
@TableName("t_trade_order")
public class TradeOrderDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("交易账号")
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("订单号")
    private Long orderId;

    @ApiModelProperty("成交单号(暂未使用)")
    private Long exchorderId;

    @ApiModelProperty("撮合单号(暂未使用)")
    private Long matchId;

    @ApiModelProperty("市场代码")
    private Integer marketType;

    @ApiModelProperty("品种代码")
    private String symbolId;

    @EnumValue(type = OrderTypeEnum.class)
    @ApiModelProperty("订单类型")
    private Integer orderType;

    @ApiModelProperty("0：buy， 1: sell")
    private Integer buyOrSell;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("挂单价格 (买入/卖出价格)")
    private BigDecimal pendingPrice;

    @ApiModelProperty("开仓价格/挂单当时市价")
    private BigDecimal openPrice;

    @ApiModelProperty("开仓时间")
    private Long openTime;

    @ApiModelProperty("真正的开仓价格")
    private BigDecimal realOpenPrice;

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

    @ApiModelProperty("备注")
    private String comment;

    @ApiModelProperty("订单过期模式")
    private Integer orderTimeMode;

    @ApiModelProperty("订单过期时间")
    private Long expiration;

    @ApiModelProperty("移动止损点数")
    private Integer dynamicSlPoint;

    @ApiModelProperty("移动止损价格")
    private BigDecimal dynamciSlPirce;

    @ApiModelProperty("交易误差")
    private Integer deviation;

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

    @ApiModelProperty("订单最新价")
    private BigDecimal askBid;

    @ApiModelProperty("涨跌幅")
    private Long priceChangeRate;

    @ApiModelProperty("买价")
    private BigDecimal askPrice;

    @ApiModelProperty("卖价")
    private BigDecimal bidPrice;

    @EnumValue(type = OrderStatusEnum.class)
    @ApiModelProperty("订单状态")
    private Integer orderStatus;

    @ApiModelProperty(value = "订单保证金")
    private BigDecimal margin;

    public static TradeOrderDO of(OrderAddParam param) {
        long now = System.currentTimeMillis();
        TradeOrderDO orderDO = new TradeOrderDO();
        orderDO.setAccountId(param.getAccountId());
        orderDO.setOpenTime(now);
        orderDO.setOrderType(param.getOperation());
        //orderDO.setBuyOrSell(BoolTypeInt.toEnum(OrderTypeEnum.BEARISH.contains(param.getOperation())));
        orderDO.setTicket(param.getTicket());
        orderDO.setStopLoss(param.getStoploss());
        orderDO.setTakeProfit(param.getTakeprofit());
        orderDO.setComment(param.getComment());
        orderDO.setExpiration(param.getExpiration());
        orderDO.setOrderTimeMode(param.getExpirationType());
        return orderDO;
    }
}