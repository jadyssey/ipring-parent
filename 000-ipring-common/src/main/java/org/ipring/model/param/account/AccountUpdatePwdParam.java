package org.ipring.model.param.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/02 19:17
 * @description:
 */
@Data
public class AccountUpdatePwdParam {

    @ApiModelProperty(value = "旧交易密码", example = "aaa")
    private String oldTradingPwd;

    @ApiModelProperty(value = "新密码", example = "aaa")
    private String newPwd;

    @ApiModelProperty(value = "账户id", example = "1")
    private Long accountId;

    @ApiModelProperty(value = "密码类型，1:交易密码，2:观摩密码", example = "1")
    private Integer passwordType;
}
