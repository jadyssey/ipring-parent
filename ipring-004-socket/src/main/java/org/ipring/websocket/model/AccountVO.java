package org.ipring.websocket.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: lgj
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
    private Long accountNum;

    @ApiModelProperty(value = "token", example = "token")
    private String token;

    @ApiModelProperty(value = "是否是观摩")
    private Boolean investor;
}
