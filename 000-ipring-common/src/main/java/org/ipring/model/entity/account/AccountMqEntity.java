package org.ipring.model.entity.account;

import org.ipring.enums.mq.MqMsgEnum;
import org.ipring.model.vo.account.AccountVO;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/13 16:08
 * @description:
 */
@Data
public class AccountMqEntity {

    private Integer mqMsgEnum;

    private Long uid;

    private Long accountId;

    private AccountVO accountVO;

    public static AccountMqEntity login(Long uid, AccountVO accountVO) {
        final AccountMqEntity entity = new AccountMqEntity();
        entity.setMqMsgEnum(MqMsgEnum.ACCOUNT_LOGIN.getType());
        entity.setUid(uid);
        entity.setAccountId(accountVO.getId());
        entity.setAccountVO(accountVO);
        return entity;
    }

    public static AccountMqEntity loginOut(Long uid) {
        final AccountMqEntity entity = new AccountMqEntity();
        entity.setMqMsgEnum(MqMsgEnum.ACCOUNT_LOGIN_OUT.getType());
        entity.setUid(uid);
        return entity;
    }
}
