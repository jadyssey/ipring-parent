package org.ipring.model.param.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AccountPwdVerifyParam {
    @ApiModelProperty(value = "账户id", example = "1")
    private Long accountId;

    @ApiModelProperty(value = "账户交易密码", example = "1")
    private String tradingPwd;
}
