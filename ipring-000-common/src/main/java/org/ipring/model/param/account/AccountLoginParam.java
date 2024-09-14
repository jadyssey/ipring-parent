package org.ipring.model.param.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author: lgj
 * @date: 2024/04/02 10:08
 * @description:
 */
@Data
public class AccountLoginParam {

    @NotNull(message = "{common.cant_null}")
    @ApiModelProperty(value = "账号id", example = "10086")
    private Long accountNum;

    @ApiModelProperty(value = "密码", example = "Yj50a03xbh")
    private String pwd;
}
