package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.SymbolMsgDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author lgj
 * @date 2024/4/2
 **/
@Getter
@AllArgsConstructor
public enum OrderTypeEnum implements IntEnumType {
    // 枚举 Sell Stop Limit  Buy Stop Limit
    BUY(0, "buy", TradeRightEnum.ONLY_LONG, OrderRightEnum.MARKET, SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice, TradeSymbolDO::getSwapLong),
    SELL(1, "sell", TradeRightEnum.ONLY_SHORT, OrderRightEnum.MARKET, SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice, TradeSymbolDO::getSwapShort),
    BUY_LIMIT(2, "buy limit", TradeRightEnum.ONLY_LONG, OrderRightEnum.PENDING, SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice, TradeSymbolDO::getSwapLong),
    SELL_LIMIT(3, "sell limit", TradeRightEnum.ONLY_SHORT, OrderRightEnum.PENDING, SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice, TradeSymbolDO::getSwapShort),
    BUY_STOP(4, "buy stop", TradeRightEnum.ONLY_LONG, OrderRightEnum.PENDING, SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice, TradeSymbolDO::getSwapLong),
    SELL_STOP(5, "sell stop", TradeRightEnum.ONLY_SHORT, OrderRightEnum.PENDING, SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice, TradeSymbolDO::getSwapShort),
    BALANCE(6, "存取款（出入金）", null, null, null, null, null),
    CREDIT(7, "信用额出/入金", null, null, null, null, null);

    public static final Map<Integer, OrderTypeEnum> ALL_ENUM_MAP =
            Arrays.stream(OrderTypeEnum.values()).collect(Collectors.toMap(OrderTypeEnum::getType, Function.identity()));

    private final Integer type;
    private final String description;
    private final TradeRightEnum tradeRight;
    private final OrderRightEnum orderRight;
    private final Function<SymbolMsgDTO, BigDecimal> openPrice;
    private final Function<SymbolMsgDTO, BigDecimal> closePrice;
    private final Function<TradeSymbolDO, BigDecimal> swapPoint;
}
