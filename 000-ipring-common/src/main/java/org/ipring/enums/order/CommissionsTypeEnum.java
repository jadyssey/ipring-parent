package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.util.CalcUtil;
import org.ipring.util.TraderCalculationUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Rainful
 * @date: 2024/04/10 17:45
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum CommissionsTypeEnum implements IntEnumType {

    FIXED_AMOUNT(1, "固定金额") {
        @Override
        public BigDecimal commissions(TradeSymbolDO symbol, Double ticket, BigDecimal marketPrice, String depositCurrency) {
            final BigDecimal commissions = CalcUtil.multiply(symbol.getCommissions(), ticket);
            return TraderCalculationUtil.currencyExchange(symbol.getCommissionsCurrency(), commissions, depositCurrency);
        }
    },

    FIXED_POINT(2, "固定点数") {
        @Override
        public BigDecimal commissions(TradeSymbolDO symbol, Double ticket, BigDecimal marketPrice, String depositCurrency) {
            return CalcUtil.divide(symbol.getCommissions(), Math.pow(10, symbol.getDigits()));
        }
    },

    TRADE_PRICE_PERCENT(3, "成交价的百分比") {
        @Override
        public BigDecimal commissions(TradeSymbolDO symbol, Double ticket, BigDecimal marketPrice, String depositCurrency) {
            return CalcUtil.multiply(symbol.getCommissions(), marketPrice);
        }
    },

    ;
    private final Integer type;
    private final String description;

    public static final Map<Integer, CommissionsTypeEnum> COMMISSIONS_TYPE_ENUM_MAP =
            Arrays.stream(CommissionsTypeEnum.values()).collect(Collectors.toMap(CommissionsTypeEnum::getType, Function.identity()));

    public abstract BigDecimal commissions(TradeSymbolDO symbol, Double ticket, BigDecimal marketPrice, String depositCurrency);
}
