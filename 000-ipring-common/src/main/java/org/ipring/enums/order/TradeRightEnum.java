package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
public enum TradeRightEnum implements IntEnumType {

    // Full Access / Only Long / Only Short / Only Close / Disabled Trade
    DISABLED_TRADE(0, " Disabled Trade"),
    ONLY_LONG(1, "Only Long"),
    ONLY_SHORT(2, "Only Short"),
    ONLY_CLOSE(4, "Only Close"),
    FULL_ACCESS(7, "Full Access"),
    ;

    public static final Map<Integer, TradeRightEnum> ALL_ENUM_MAP =
            Arrays.stream(TradeRightEnum.values()).collect(Collectors.toMap(TradeRightEnum::getType, Function.identity()));

    public static boolean tradeRightLimit(Integer operation, Integer tradeRight) {
        final TradeRightEnum tradeType = OrderTypeEnum.ALL_ENUM_MAP.get(operation).getTradeRight();
        return tradeType == null || (tradeType.getType() & tradeRight) != 0;
    }

    private final Integer type;
    private final String description;
}
