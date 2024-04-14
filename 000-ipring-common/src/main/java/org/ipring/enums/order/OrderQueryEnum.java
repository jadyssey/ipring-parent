package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 查询历史订单筛选枚举类
 *
 * @author lgj
 * @date 2024/4/2
 **/
@Getter
@AllArgsConstructor
public enum OrderQueryEnum implements IntEnumType {
    ALL(1, "全部", Collections.emptyList()),
    MARKET(2, "市价单", Arrays.asList(OrderTypeEnum.BUY.getType(), OrderTypeEnum.SELL.getType())),
    PENDING(3, "挂单", Arrays.asList(OrderTypeEnum.BUY_LIMIT.getType(), OrderTypeEnum.BUY_STOP.getType(), OrderTypeEnum.SELL_LIMIT.getType(), OrderTypeEnum.SELL_STOP.getType())),
    BALANCE(4, "存取款", Collections.singletonList(OrderTypeEnum.BALANCE.getType())),
    CREDIT(5, "信用额", Collections.singletonList(OrderTypeEnum.CREDIT.getType())),
    ;

    public static final Map<Integer, OrderQueryEnum> ALL_ENUM_MAP =
            Arrays.stream(OrderQueryEnum.values()).collect(Collectors.toMap(OrderQueryEnum::getType, Function.identity()));

    private final Integer type;
    private final String description;
    private final List<Integer> orderTypeList;
}
