package org.ipring.model.param.account;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.ipring.anno.EnumValue;
import org.ipring.enums.account.CurrencyEnum;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author: lgj
 * @date: 2024/04/02 10:03
 * @description:
 */
@Data
public class AccountAddParam {

    @ApiModelProperty(value = "账户名", example = "aaa")
    @NotEmpty(message = "{account.name.error}")
    @Length(min = 1, max = 64, message = "{account.name.error}")
    private String name;
    @ApiModelProperty(value = "区号", example = "0086")
    @NotEmpty(message = "{common.cant_null}")
    private String areaCode;
    @ApiModelProperty(value = "手机号", example = "10000")
    @NotEmpty(message = "{account.phoneNum.error}")
    private String phoneNum;
    @ApiModelProperty(value = "邮箱", example = "aaa@apple.com")
    @NotEmpty(message = "{account.email.error}")
    @Email(message = "{common.email.error}")
    private String email;  // todo 项目中用到的所有邮箱是否都要转小写入库，否则会出问题
    @ApiModelProperty(value = "初始资金", example = "1000.00")
    @NotNull(message = "{common.cant_null}")
    @Range(min = 100, max = 10000000, message = "{common.illegal}")
    @Digits(integer = 8, fraction = 0, message = "{common.illegal}")
    private BigDecimal initFund;
    @ApiModelProperty(value = "存款货币", example = "USD")
    @EnumValue(type = CurrencyEnum.class, nullable = false)
    private String depositCurrency;
    @ApiModelProperty(value = "杠杆", example = "100")
    @NotNull(message = "{common.cant_null}")
    @Range(min = 1, max = 800, message = "{common.illegal}")
    private Integer lever;
    @ApiModelProperty(value = "交易服务器id", example = "1")
    @NotNull(message = "{common.cant_null}")
    private Integer tradingServerId;
}
