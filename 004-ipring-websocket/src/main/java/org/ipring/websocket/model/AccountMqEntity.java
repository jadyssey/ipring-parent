package org.ipring.websocket.model;

import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/13 16:08
 * @description:
 */
@Data
public class AccountMqEntity {

    private Integer mqMsgEnum;

    private String token;

    private Long accountId;

    private AccountVO accountVO;

    public static AccountMqEntity login(String token, AccountVO accountVO) {
        final AccountMqEntity entity = new AccountMqEntity();
        entity.setMqMsgEnum(MqMsgEnum.ACCOUNT_LOGIN.getType());
        entity.setToken(token);
        entity.setAccountId(accountVO.getId());
        entity.setAccountVO(accountVO);
        return entity;
    }

    public static AccountMqEntity loginOut(String token, Long accountId) {
        final AccountMqEntity entity = new AccountMqEntity();
        entity.setMqMsgEnum(MqMsgEnum.ACCOUNT_LOGIN_OUT.getType());
        entity.setToken(token);
        entity.setAccountId(accountId);
        return entity;
    }

    public static AccountMqEntity id(Long accountId) {
        final AccountMqEntity entity = new AccountMqEntity();
        entity.setMqMsgEnum(MqMsgEnum.ACCOUNT_BALANCE.getType());
        entity.setAccountId(accountId);
        return entity;
    }
}
