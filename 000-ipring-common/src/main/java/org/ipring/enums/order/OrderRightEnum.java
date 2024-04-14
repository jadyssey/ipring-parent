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
public enum OrderRightEnum implements IntEnumType {

    // 1 Market 2 Pending 4 Stoploss 8 TakeProfit 用4bit表示
    MARKET(1, "允许开 Buy/Sell。如果该权限被禁，不允许开 Buy/Sell，但已开出的市价单保留"),
    PENDING(2, "允许开 Stop/Limit/Stop Limit。如果该权限被禁，不允许开 Stop/Limit/Stop Limit，但已开出的挂单保留，且不可修改"),
    STOPLOSS(4, "允许设置止损。如果该权限被禁，不允许设置止损，但已设置的止损保留，且不可修改止损"),
    TAKEPROFIT(8, "允许设置止盈。如果该权限被禁，不允许设置止盈，但已设置的止盈保留，且不可修改止盈"),
    ;

    public static final Map<Integer, OrderRightEnum> ALL_ENUM_MAP =
            Arrays.stream(OrderRightEnum.values()).collect(Collectors.toMap(OrderRightEnum::getType, Function.identity()));

    public static boolean orderRightLimit(Integer operation, Integer orderRight) {
        final OrderRightEnum tradeType = OrderTypeEnum.ALL_ENUM_MAP.get(operation).getOrderRight();
        return tradeType == null || (tradeType.getType() & orderRight) != 0;
    }

    private final Integer type;
    private final String description;
}
