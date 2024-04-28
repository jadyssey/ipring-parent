package org.ipring.websocket;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/15 14:41
 * @description:
 */
@Data
public class WebsocketAccRecvEntity {

    @ApiModelProperty(value = "账户id", example = "[1,2]")
    public List<Long> accIds;
}
