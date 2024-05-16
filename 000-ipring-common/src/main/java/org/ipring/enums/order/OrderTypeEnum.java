package org.ipring.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipring.enums.IntEnumType;
import org.ipring.model.SymbolMsgDTO;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
    BUY(0, "Buy", SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice),
    SELL(1, "Sell", SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice),
    BUY_LIMIT(2, "Buy Limit", SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice),
    SELL_LIMIT(3, "Sell Limit", SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice),
    BUY_STOP(4, "Buy Stop", SymbolMsgDTO::getBidPrice, SymbolMsgDTO::getAskPrice),
    SELL_STOP(5, "Sell Stop", SymbolMsgDTO::getAskPrice, SymbolMsgDTO::getBidPrice),
    BALANCE(6, "存取款（出入金）", null, null),
    CREDIT(7, "信用额出/入金", null, null);

    public static final Map<Integer, OrderTypeEnum> ALL_ENUM_MAP =
            Arrays.stream(OrderTypeEnum.values()).collect(Collectors.toMap(OrderTypeEnum::getType, Function.identity()));

    public static final List<Integer> BUY_LIST = Arrays.asList(BUY.getType(), BUY_LIMIT.getType(), BUY_STOP.getType());
    public static final List<Integer> SELL_LIST = Arrays.asList(SELL.getType(), SELL_LIMIT.getType(), SELL_STOP.getType());

    private final Integer type;
    private final String description;
    private final Function<SymbolMsgDTO, BigDecimal> openPrice;
    private final Function<SymbolMsgDTO, BigDecimal> closePrice;
}
