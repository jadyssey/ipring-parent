package org.ipring.model.param.order;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/8
 **/

@Data
public class OrderCloseParam {

    @ApiModelProperty("交易账号")
    @NotNull(message = "{common.cant_null}")
    private Long accountId;

    @ApiModelProperty("订单号")
    @NotNull(message = "{common.cant_null}")
    private Long orderId;

    @ApiModelProperty("交易手数")
    @NotNull(message = "{common.cant_null}")
    private Double ticket;

    @ApiModelProperty("请求的成交价格")
    private BigDecimal price;
}
