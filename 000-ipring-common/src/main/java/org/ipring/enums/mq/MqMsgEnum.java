package org.ipring.enums.mq;

import org.ipring.enums.IntEnumType;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.model.entity.account.AccountMqEntity;
import org.ipring.model.entity.ws.WebSocketCmd;
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
public enum MqMsgEnum implements IntEnumType {

    ACCOUNT_LOGIN(1, "account login") {
        @Override
        public WebSocketCmd trans(AccountMqEntity entity) {
            return WebSocketCmd.account(MqMsgEnum.ACCOUNT_LOGIN, entity.getAccountVO());
        }
    },
    ACCOUNT_LOGIN_OUT(2, "account login out") {
        @Override
        public WebSocketCmd trans(AccountMqEntity entity) {
            return WebSocketCmd.account(MqMsgEnum.ACCOUNT_LOGIN_OUT, entity.getAccountId());
        }
    },
    ORDER_REFRESH(11, "order data refresh"),
    ORDER_PROFIT(12, "order profit"),
    ;

    private final Integer type;
    private final String description;

    public static final Map<Integer, MqMsgEnum> MQ_MSG_ENUM_MAP =
            Arrays.stream(MqMsgEnum.values()).collect(Collectors.toMap(MqMsgEnum::getType, Function.identity()));

    public WebSocketCmd trans(AccountMqEntity entity) {
        throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
    }
}
