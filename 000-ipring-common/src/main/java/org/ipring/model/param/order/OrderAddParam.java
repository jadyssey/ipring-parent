package org.ipring.model.param.order;

import org.ipring.anno.EnumValue;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.enums.order.ExpirationTypeEnum;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.entity.SymbolUniqEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/2
 **/
@Data
public class OrderAddParam extends SymbolUniqEntity {
    @ApiModelProperty("账号id")
    @NotNull(message = "{common.cant_null}")
    private Long accountId;

    @EnumValue(type = OrderTypeEnum.class, nullable = false)
    @ApiModelProperty("交易方向/订单类型")
    private Integer operation;

    @ApiModelProperty("交易手数")
    @NotNull(message = "{common.cant_null}")
    @Min(value = 0, message = "{common.illegal}")
    private Double ticket;

    @ApiModelProperty("请求的成交价格 （市价单时为实时价，挂单时为挂单的买卖价）")
    @NotNull(message = "{common.cant_null}")
    @Min(value = 0, message = "{common.illegal}")
    private BigDecimal price;

    @ApiModelProperty("滑点误差（按点数给，无限制可传0）")
    private Long slippage;

    @ApiModelProperty("止损价格")
    private BigDecimal stoploss;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeprofit;

    @ApiModelProperty("备注信息")
    private String comment;

    @ApiModelProperty("订单过期模式")
    @EnumValue(type = ExpirationTypeEnum.class, nullable = false)
    private Integer expirationType;

    @ApiModelProperty("订单过期时间（13位时间戳）")
    private Long expiration;

    @ApiModelProperty("是否允许盘前盘后成交")
    @EnumValue(type = BoolTypeInt.class)
    @NotNull(message = "{common.cant_null}")
    private Integer overTrade;
}
