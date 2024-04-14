package org.ipring.model.vo.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AccountDetailVO {

    @ApiModelProperty(value = "账户名", example = "aaa")
    private String name;

    @ApiModelProperty(value = "杠杆", example = "100")
    private Integer lever;

    @ApiModelProperty(value = "存款货币", example = "USD")
    private String depositCurrency;

    @ApiModelProperty(value = "爆仓比例", example = "1.0")
    private String marginCallRate;

    @ApiModelProperty(value = "交易商名", example = "aaa")
    private String brokerName;

    @ApiModelProperty(value = "交易商logo", example = "aaa")
    private String brokerLogo;

    @ApiModelProperty(value = "服务器", example = "aaa")
    private String tradingServerName;

    @ApiModelProperty(value = "账户id", example = "1")
    private Long accountId;
}
