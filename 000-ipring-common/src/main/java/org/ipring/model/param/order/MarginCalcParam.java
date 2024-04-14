package org.ipring.model.param.order;

import org.ipring.anno.EnumValue;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.entity.SymbolUniqEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author: Rainful
 * @date: 2024/04/11 15:25
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MarginCalcParam extends SymbolUniqEntity {

    @ApiModelProperty(value = "账户id", example = "1")
    @NotNull(message = "{common.cant_null}")
    private Long accountId;

    @ApiModelProperty(value = "手数", example = "0.1")
    @NotNull(message = "{common.cant_null}")
    private Double ticket;

    @ApiModelProperty(value = "订单类型 一般来说只要 buy和sell就好了 不需要关心是不是挂单", example = "1")
    @EnumValue(type = OrderTypeEnum.class, nullable = false, message = "{common.illegal}")
    private Integer orderType;
}
