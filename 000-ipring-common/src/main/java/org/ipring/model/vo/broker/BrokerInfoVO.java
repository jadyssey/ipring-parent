package org.ipring.model.vo.broker;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BrokerInfoVO {
    @ApiModelProperty(value = "交易商名称", example = "aaa")
    private String brokerName;

    @ApiModelProperty(value = "交易商Logo", example = "aaa")
    private String brokerLogo;

    @ApiModelProperty(value = "交易商服务器地址", example = "aaa")
    private String tradingServerName;

    @ApiModelProperty(value = "服务器id", example = "1")
    private Integer serverId;

}
