package org.ipring.model.httpclient.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author lgj
 * @date 2024/4/2
 **/
@Data
public class OrderAddDTO extends SymbolUniqEntity {
    @ApiModelProperty("账号id")
    private Long accountId;

    @ApiModelProperty("交易方向/订单类型")
    private Integer operation;

    @ApiModelProperty("交易手数")
    private Double ticket;

    @ApiModelProperty("请求的成交价格 （市价单时为实时价，挂单时为挂单的买卖价）")
    private BigDecimal price;

    @ApiModelProperty("滑点误差（按点数给，无限制则不传或传空）")
    private Long slippage;

    @ApiModelProperty("止损价格")
    private BigDecimal stoploss;

    @ApiModelProperty("止盈价格")
    private BigDecimal takeprofit;

    @ApiModelProperty("备注信息")
    private String comment;

    @ApiModelProperty("订单过期模式")
    private Integer expirationType;

    @ApiModelProperty("订单过期时间（13位时间戳，单位：毫秒）")
    private Long expiration;

    @ApiModelProperty("是否允许盘前盘后成交")
    private Integer overTrade;

    @ApiModelProperty("是否跟单")
    private Integer followed;

    @ApiModelProperty("被跟单用户")
    private Long signalAccountId;
}
