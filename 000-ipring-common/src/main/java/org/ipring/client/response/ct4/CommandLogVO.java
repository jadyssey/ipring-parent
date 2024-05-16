package org.ipring.client.response.ct4;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/22 18:17
 * @description:
 */
@Data
public class CommandLogVO {

    @ApiModelProperty(value = "请求指令", example = "Buy Limit 1.00 USDJPY at 109.824 sl: 0.000 tp: 0.000")
    private String req;

    @ApiModelProperty(value = "响应指令", example = "Buy Limit 1.00 USDJPY at 109.824 sl: 0.000 tp: 0.000 Successed (Order #432523)")
    private String res;
}
