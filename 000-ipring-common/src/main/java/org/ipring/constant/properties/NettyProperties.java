package org.ipring.constant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lgj
 * @date 2024/4/3
 **/
@Data
@ConfigurationProperties(prefix = "ipring.netty")
public class NettyProperties {

    /**
     * 服务端地址
     */
    private String server;
}
