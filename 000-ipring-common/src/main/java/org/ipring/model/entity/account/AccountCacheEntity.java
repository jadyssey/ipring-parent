package org.ipring.model.entity.account;

import org.ipring.enums.CommonStateEnum;
import org.ipring.enums.account.AccountRoleEnum;
import org.ipring.model.dobj.account.AccountDO;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/02 20:18
 * @description:
 */
@Data
public class AccountCacheEntity {

    private Integer role;
    private Integer state;
    private String depositCurrency; // 这个属性取值要在每个账号的-1值里面取 不要取用户id里面取
    private Integer lever;

    public static AccountCacheEntity of(AccountDO accountDO) {
        final AccountCacheEntity entity = new AccountCacheEntity();
        entity.setDepositCurrency(accountDO.getDepositCurrency());
        entity.setLever(accountDO.getLever());
        return entity;
    }

    public static AccountCacheEntity of(AccountDO accountDO, AccountRoleEnum roleEnum) {
        final AccountCacheEntity entity = new AccountCacheEntity();
        entity.setRole(roleEnum.getType());
        entity.setState(CommonStateEnum.UP.getType());
        return entity;
    }

    public static AccountCacheEntity logOut() {
        final AccountCacheEntity entity = new AccountCacheEntity();
        entity.setState(CommonStateEnum.DOWN.getType());
        return entity;
    }
}
