package org.ipring.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Rainful
 * @date: 2024/04/07 10:05
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum MqMsgEnum {

    ACCOUNT_LOGIN(1, "account login"),
    ACCOUNT_LOGIN_OUT(2, "account login out") ,
    ACCOUNT_BALANCE(3, "账户余额变动 一般来说是账户服务发给交易服务的 交易服务需要新增一条出入金记录"),
    ORDER_REFRESH(11, "order data refresh"),
    ORDER_PROFIT(12, "order profit"),
    ;

    private final Integer type;
    private final String description;

    public static final Map<Integer, MqMsgEnum> MQ_MSG_ENUM_MAP =
            Arrays.stream(MqMsgEnum.values()).collect(Collectors.toMap(MqMsgEnum::getType, Function.identity()));
}
