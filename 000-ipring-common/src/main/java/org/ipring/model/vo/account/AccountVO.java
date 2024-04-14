package org.ipring.model.vo.account;

import org.ipring.model.dobj.account.AccountDO;
import org.ipring.model.dobj.server.ServerDO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/02 9:35
 * @description:
 */
@Data
public class AccountVO {

    @ApiModelProperty(value = "id", example = "1")
    private Long id;

    @ApiModelProperty(value = "账户名", example = "aaa")
    private String name;

    @ApiModelProperty(value = "杠杆", example = "100")
    private Integer lever;

    @ApiModelProperty(value = "存款货币", example = "USD")
    private String depositCurrency;

    @ApiModelProperty(value = "交易密码", example = "aaa")
    private String tradingPwd;

    @ApiModelProperty(value = "观摩密码", example = "aaa")
    private String viewPwd;

    @ApiModelProperty(value = "交易商名", example = "aaa")
    private String brokerName;

    @ApiModelProperty(value = "服务器", example = "aaa")
    private String tradingServerName;

    @ApiModelProperty(value = "交易商id", example = "1")
    private Integer brokerId;

    @ApiModelProperty(value = "初始资金", example = "100")
    private BigDecimal initFund;

    @ApiModelProperty(value = "交易商服务器id", example = "1")
    private Integer brokerServerId;

    @ApiModelProperty(value = "爆仓比例", example = "1")
    private BigDecimal marginCallRate;

    @ApiModelProperty(value = "邮箱", example = "1")
    private String email;

    @ApiModelProperty(value = "账户号", example = "123")
    private Long accountId;

    public static AccountVO of(AccountDO accountDO, ServerDO serverDO) {
        final AccountVO vo = new AccountVO();
        vo.setId(accountDO.getId());
        vo.setName(accountDO.getName());
        vo.setLever(accountDO.getLever());
        vo.setDepositCurrency(accountDO.getDepositCurrency());
        vo.setTradingPwd(accountDO.getTradingPwd());
        vo.setViewPwd(accountDO.getViewPwd());
        vo.setBrokerName(serverDO.getBrokerName());
        vo.setTradingServerName(serverDO.getServerName());
        vo.setBrokerId(serverDO.getBrokerId());
        vo.setInitFund(accountDO.getInitFund());
        vo.setBrokerServerId(serverDO.getServerId());
        vo.setMarginCallRate(accountDO.getMarginCall());
        vo.setEmail(accountDO.getEmail());
        vo.setAccountId(accountDO.getAccountNum());
        return vo;
    }

    public static AccountVO of(Long id) {
        final AccountVO vo = new AccountVO();
        vo.setId(id);
        return vo;
    }
}
