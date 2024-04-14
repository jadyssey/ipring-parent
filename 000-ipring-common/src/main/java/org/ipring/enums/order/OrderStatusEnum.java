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
public enum OrderStatusEnum implements IntEnumType {
    // 订单状态
    CREAT(1, "创建状态", true, true),
    PENDING(2, "未成交(挂单)", true, true),
    EXPIRED(3, "已过期", false, false),
    TAKE(5, "已成交", true, false),
    CLOSE(9, "已平仓", false, false),
    DEL(10, "已删除", false, true),
    ;

    public static final Map<Integer, OrderStatusEnum> ALL_ENUM_MAP =
            Arrays.stream(OrderStatusEnum.values()).collect(Collectors.toMap(OrderStatusEnum::getType, Function.identity()));

    public static boolean unDeal(Integer status) {
        return OrderStatusEnum.PENDING.type.equals(status) || OrderStatusEnum.CREAT.type.equals(status);
    }

    private final Integer type;
    private final String description;

    /**
     * 可更新的订单就是实时的订单，不可更新的订单就是历史订单
     */
    private final boolean realtime;
    private final boolean delAuth;
}
