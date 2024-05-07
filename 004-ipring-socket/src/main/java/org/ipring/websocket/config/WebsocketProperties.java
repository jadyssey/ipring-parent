package org.ipring.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: lgj
 * @date: 2024/04/03 13:50
 * @description:
 */
@ConfigurationProperties(prefix = "websocket")
@Data
public class WebsocketProperties {

    /**
     * websocket 端口
     */
    private Integer port;

    /**
     * websocket path
     */
    private String path;

    /**
     * 链接上限
     */
    private Integer limitCon = 10240;

    /**
     * 心跳时间
     */
    private Integer heartBeatGap = 10;

    /**
     * 心跳检查时间 超过这么多时间客户端没有相应心跳 就会被断开连接
     */
    private Integer heartBeatCheck = 33;
}
