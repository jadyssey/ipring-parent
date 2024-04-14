package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.TradeOrderEntity;
import org.ipring.util.CalcUtil;
import org.ipring.util.TraderCalculationUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lgj
 * @date 2024/4/10
 **/
@Getter
@AllArgsConstructor
public enum SwapTypeEnum implements IntEnumType {
    // 库存费类型
    // 关于此处的汇率转换，一般来说
    // 1. 基础货币 = 预付款货币
    // 2. 计价货币 = 盈亏货币
    POINT(0, "按点数 (点数通常用于表示价格变动的最小单位)") {
        @Override
        public BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
            BigDecimal swapValue = OrderTypeEnum.ALL_ENUM_MAP.get(order.getOrderType()).getSwapPoint().apply(symbol);
            if (Objects.isNull(swapValue)) return BigDecimal.ZERO;
            Double contract = symbol.getContractSize() * order.getTicket();
            BigDecimal swapUnit = CalcUtil.multiply(swapRate(swapValue, symbol.getDigits()), contract);
            // swapUnit 属于计价货币单位，需要转为账号存款货币单位
            return TraderCalculationUtil.currencyExchange(symbol.getProfitCurrency(), swapUnit, depositCurrency);
        }
    },
    BASE(1, "按基础货币") {
        @Override
        public BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
            BigDecimal swapValue = OrderTypeEnum.ALL_ENUM_MAP.get(order.getOrderType()).getSwapPoint().apply(symbol);
            if (Objects.isNull(swapValue)) return BigDecimal.ZERO;
            return TraderCalculationUtil.currencyExchange(symbol.getMarginCurrency(), swapValue, depositCurrency);
        }
    },
    PROFIT(2, "按盈亏货币") {
        @Override
        public BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
            BigDecimal swapValue = OrderTypeEnum.ALL_ENUM_MAP.get(order.getOrderType()).getSwapPoint().apply(symbol);
            if (Objects.isNull(swapValue)) return BigDecimal.ZERO;
            return TraderCalculationUtil.currencyExchange(symbol.getProfitCurrency(), swapValue, depositCurrency);
        }
    },
    MARGIN(3, "按预付款货币") {
        @Override
        public BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
            BigDecimal swapValue = OrderTypeEnum.ALL_ENUM_MAP.get(order.getOrderType()).getSwapPoint().apply(symbol);
            if (Objects.isNull(swapValue)) return BigDecimal.ZERO;
            return TraderCalculationUtil.currencyExchange(symbol.getMarginCurrency(), swapValue, depositCurrency);
        }
    },
    DEPOSIT(4, "按存款货币") {
        @Override
        public BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
            BigDecimal swapValue = OrderTypeEnum.ALL_ENUM_MAP.get(order.getOrderType()).getSwapPoint().apply(symbol);
            if (Objects.isNull(swapValue)) return BigDecimal.ZERO;
            // 无需转换汇率
            return swapValue;
        }
    },
    ;

    public static final Map<Integer, SwapTypeEnum> ALL_ENUM_MAP =
            Arrays.stream(SwapTypeEnum.values()).collect(Collectors.toMap(SwapTypeEnum::getType, Function.identity()));

    public abstract BigDecimal calcSwap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency);

    /**
     * 掉期率
     * 一个合约需要缴纳的利息金额
     *
     * @param swapPoint 隔夜利息点数   一个合约需要缴纳的利息点数
     * @param digits    报价小数点位数
     */
    private static BigDecimal swapRate(BigDecimal swapPoint, Integer digits) {
        return CalcUtil.multiply(swapPoint, CalcUtil.pow(10, -digits));
    }

    private final Integer type;
    private final String description;
}
