package org.ipring.model.dobj.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ipring.model.param.account.AccountAddParam;
import org.ipring.util.DateUtil;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/02 11:00
 * @description:
 */
@Data
@TableName("t_ac_account")
@FieldNameConstants
public class AccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountNum;
    private String name;
    private String areaCode;
    private String phoneNum;
    private String email;
    private BigDecimal initFund;
    private String tradingPwd;
    private String viewPwd;
    private Integer broker;
    private Integer tradingServer;
    private Integer type;
    private Integer lever;
    private String depositCurrency;
    private BigDecimal marginCall;
    private BigDecimal stopOutLevel;
    private Long createTime;
    private Long updateTime;
    private Integer state;
    private Long creator;
    private Integer del;

    public static AccountDO of(AccountAddParam param, String tradingPwd, String viewPwd, Integer broker, Integer type,
                               BigDecimal marginCall, BigDecimal stopOutLevel, Integer state, Long uid, Long accountNum) {

        final AccountDO accountDO = new AccountDO();
        accountDO.setName(param.getName());
        accountDO.setAreaCode(param.getAreaCode());
        accountDO.setPhoneNum(param.getPhoneNum());
        accountDO.setEmail(param.getEmail());
        accountDO.setInitFund(param.getInitFund());
        accountDO.setTradingPwd(tradingPwd);
        accountDO.setViewPwd(viewPwd);
        accountDO.setBroker(broker);
        accountDO.setTradingServer(param.getTradingServerId());
        accountDO.setType(type);
        accountDO.setLever(param.getLever());
        accountDO.setDepositCurrency(param.getDepositCurrency());
        accountDO.setMarginCall(marginCall);
        accountDO.setStopOutLevel(stopOutLevel);
        accountDO.setAccountNum(accountNum);

        final long now = DateUtil.getMills();
        accountDO.setCreateTime(now);
        accountDO.setUpdateTime(now);
        accountDO.setState(state);
        accountDO.setCreator(uid);
        return accountDO;
    }

    public static AccountDO delObj(Long id) {
        final AccountDO accountDO = new AccountDO();
        accountDO.setId(id);
        return accountDO;
    }
}
