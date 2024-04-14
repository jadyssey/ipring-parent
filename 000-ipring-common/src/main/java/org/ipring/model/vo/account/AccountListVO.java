package org.ipring.model.vo.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/02 9:35
 * @description:
 */
@Data
public class AccountListVO {

    @ApiModelProperty(value = "id", example = "1")
    private Long id;

    @ApiModelProperty(value = "账户号", example = "123")
    private Long accountNum;

    @ApiModelProperty(value = "类型", example = "1真实 2模拟")
    private Integer type;

    @ApiModelProperty(value = "账户名", example = "aaa")
    private String name;

    @ApiModelProperty(value = "创建时间 13位时间戳", example = "1704038400000")
    private Long createTime;

    @ApiModelProperty(value = "杠杆", example = "100")
    private Integer lever;

    @ApiModelProperty(value = "爆仓比例", example = "0.5")
    private BigDecimal stopOutLevel;

    @ApiModelProperty(value = "存款货币", example = "USD")
    private String depositCurrency;

    @ApiModelProperty(value = "累计入金", example = "199.00")
    private BigDecimal accumulateDeposit;

    @ApiModelProperty(value = "累计出金", example = "199.00")
    private BigDecimal accumulateWithdraw;

    @ApiModelProperty(value = "交易商名", example = "aaa")
    private String brokerName;

    @ApiModelProperty(value = "服务器", example = "aaa")
    private String tradingServerName;

    @ApiModelProperty(value = "是否是创建人", example = "true")
    private Boolean creator;

    @ApiModelProperty(value = "角色 1交易角色 2观摩角色", example = "1")
    private Integer role;

    @ApiModelProperty(value = "交易商Logo", example = "aaa")
    private String brokerLogo;
}
