package org.ipring.model.dobj.trade;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ipring.anno.EnumValue;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.enums.order.CalculationEnum;
import org.ipring.enums.order.CommissionsTypeEnum;
import org.ipring.enums.order.ExpirationTypeEnum;
import org.ipring.enums.order.OrderOpenAndCloseEnum;
import org.ipring.enums.order.OrderRightEnum;
import org.ipring.enums.order.SwapTypeEnum;
import org.ipring.enums.order.TradeRightEnum;
import org.ipring.model.inte.symbol.SymbolInfoInte;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

/**
 * @author lgj
 * @description
 * @date 2024-04-02
 */
@Data
@TableName("t_trade_symbol")
@FieldNameConstants
public class TradeSymbolDO implements SymbolInfoInte {
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("市场代码 (https://www.kdocs.cn/l/co74skpt5a4h)")
    private Integer marketType;

    @ApiModelProperty("品种代码（合约代码）")
    private String symbolId;

    @ApiModelProperty("放大倍数")
    private Long multiple;

    @ApiModelProperty("报价的小数点位数")
    private Integer digits;

    @ApiModelProperty("一手的股数")
    private Long contractSize;

    @ApiModelProperty("买卖价之间的点差")
    private Integer spread;

    @ApiModelProperty("停损级别，表示挂单的挂单价格或者市价单的止盈止损价格至少需要距离当前市价多少点或者挂单的止盈止损必须距离挂单价格多少点。")
    private Integer stopsLevel;

    @ApiModelProperty("图表绘制类型 0买价 1卖价 2最后价")
    private Integer chartMode;

    @ApiModelProperty("预付款货币")
    private String marginCurrency;

    @ApiModelProperty("盈亏货币")
    private String profitCurrency;

    @ApiModelProperty("minimum price change step")
    private Integer tickSize;

    @ApiModelProperty("cost of a single price change point")
    private Long tickPrice;

    @ApiModelProperty(value = "结算模式", example = "1")
    @EnumValue(type = CalculationEnum.class)
    private Integer calculation;

    @ApiModelProperty("初始预付款")
    private BigDecimal initialMargin;

    @ApiModelProperty("维持预付款")
    private BigDecimal maintenanceMargin;

    @ApiModelProperty("预付款金比率")
    private BigDecimal marginRate;

    @ApiModelProperty("成交模式 固定值为instant(写0就好了)")
    private Integer matchMode;

    /**
     * @see TradeRightEnum
     */
    @ApiModelProperty("交易权限")
    @EnumValue(type = TradeRightEnum.class)
    private Integer tradeRight;

    @ApiModelProperty("订单权限")
    @EnumValue(type = OrderRightEnum.class)
    private Integer orderRight;

    @ApiModelProperty("填充模式 fok(fill or kill 要么全部成交，要么返回失败) ioc(immediate or cancel 可部分成交)")
    private Integer fillingType;

    @ApiModelProperty("过期类型")
    @EnumValue(type = ExpirationTypeEnum.class)
    private Integer expirationType;

    @ApiModelProperty("手续费 1按固定金额、2按固定点数、3按成交价值的百分比")
    private String fee;

    @ApiModelProperty("最小交易量手数")
    private Double minVolume;

    @ApiModelProperty("最大交易量手数")
    private Double maxVolume;

    @ApiModelProperty("交易量步长")
    private Double volumeStep;

    @ApiModelProperty("库存费是否存在")
    private Integer swapStatus;

    @ApiModelProperty("库存费类型")
    @EnumValue(type = SwapTypeEnum.class)
    private Integer swapType;

    @ApiModelProperty("买单库存费 (多单和空单的库存费收取标准是不一样的，需要分别定义)")
    private BigDecimal swapLong;

    @ApiModelProperty("卖单库存费 (多单和空单的库存费收取标准是不一样的，需要分别定义)")
    private BigDecimal swapShort;

    @ApiModelProperty("三日库存费")
    private Integer threedaysSwap;

    @ApiModelProperty("交易时段 premarket->盘前交易，afterhours->盘后交易，opencompetition->开盘集合竞价，closecompetition->收盘集合竞价，pricing->盘后定价交易，quetes->行情时段，trade->交易时段")
    private String tradeSessions;

    @ApiModelProperty("交易时段8时区")
    private String tradeSessionsUtc8;

    @ApiModelProperty("涨跌幅限制")
    private Integer limitRate;

    @ApiModelProperty("是否激活状态")
    @EnumValue(type = BoolTypeInt.class)
    private Integer active;

    @ApiModelProperty("手续费模式")
    @EnumValue(type = CommissionsTypeEnum.class)
    private Integer commissionsMode;

    @ApiModelProperty("手续费时机")
    @EnumValue(type = OrderOpenAndCloseEnum.OrderTimingEnum.class)
    private Integer commissionsTiming;

    @ApiModelProperty("手机费值")
    private BigDecimal commissions;

    @ApiModelProperty("手续费单位")
    private String commissionsCurrency;
}