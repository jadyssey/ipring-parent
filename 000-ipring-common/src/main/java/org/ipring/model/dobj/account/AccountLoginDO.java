package org.ipring.model.dobj.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ipring.enums.account.AccountRoleEnum;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/02 14:47
 * @description:
 */
@Data
@TableName("t_ac_login")
public class AccountLoginDO {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Long uid;
    private Long accountId;
    private Integer del;
    private Integer role;

    public static AccountLoginDO of(Long uid, Long accountId, AccountRoleEnum roleEnum) {
        final AccountLoginDO aDo = new AccountLoginDO();
        aDo.setUid(uid);
        aDo.setAccountId(accountId);
        aDo.setRole(roleEnum.getType());
        return aDo;
    }

    public static AccountLoginDO del(Integer id) {
        final AccountLoginDO aDo = new AccountLoginDO();
        aDo.setId(id);
        aDo.setDel(1);
        return aDo;
    }
}
