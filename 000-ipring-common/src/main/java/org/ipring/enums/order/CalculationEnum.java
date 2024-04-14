package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.inte.order.OrderInfoInte;
import org.ipring.model.inte.symbol.SymbolInfoInte;
import org.ipring.util.CalcUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Rainful
 * @date: 2024/04/09 14:10
 * @description:
 */

@RequiredArgsConstructor
@Getter
public enum CalculationEnum implements IntEnumType {

    CFD(0, "差价合约") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            return CalcUtil.multiply(marketPrice, symbol.getContractSize());
        }
    },
    CFD_LEVER(1, "差价合约-杠杆模式") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            return CalcUtil.divide(CFD.calcMargin(symbol, marketPrice, accountLever), accountLever);
        }
    },
    CFD_INDEX(2, "差价合约-指数模式") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            final BigDecimal temp = CalcUtil.multiply(CFD.calcMargin(symbol, marketPrice, accountLever), symbol.getTickPrice());
            return CalcUtil.divide(temp, symbol.getTickSize());
        }
    },
    FUTURE(3, "期货模式") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            return symbol.getInitialMargin();
        }

        @Override
        public BigDecimal calcTradeProfit(BigDecimal priceDiff, SymbolInfoInte symbol, OrderInfoInte order) {
            // priceDiff * TicketPrice / TicketSize * Lots 主要是先乘后除 减少误差值
            final BigDecimal temp1 = CalcUtil.multiply(symbol.getTickPrice(), order.getTicket());
            final BigDecimal temp2 = CalcUtil.multiply(priceDiff, temp1);
            return CalcUtil.divide(temp2, symbol.getTickSize());
        }
    },
    FOREX(4, "外汇模式") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            return CalcUtil.divide(symbol.getContractSize(), accountLever);
        }
    },
    FOREX_NO_LEVER(5, "外汇模式-无杠杆") {
        @Override
        public BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever) {
            return BigDecimal.valueOf(symbol.getContractSize());
        }
    },

    ;
    private final Integer type;
    private final String description;

    public static final Map<Integer, CalculationEnum> CALCULATION_ENUM_MAP =
            Arrays.stream(CalculationEnum.values()).collect(Collectors.toMap(CalculationEnum::getType, Function.identity()));

    public abstract BigDecimal calcMargin(TradeSymbolDO symbol, BigDecimal marketPrice, Integer accountLever);

    public BigDecimal calcTradeProfit(BigDecimal priceDiff, SymbolInfoInte symbol, OrderInfoInte order) {
        final BigDecimal size = CalcUtil.multiply(symbol.getContractSize(), order.getTicket());
        return CalcUtil.multiply(priceDiff, size);
    }
}
