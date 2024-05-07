package org.ipring.tcp.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: lgj
 * @date: 2024/04/03 18:16
 * @description:
 */
@Data
@ConfigurationProperties(prefix = "tcp")
public class TcpServerProperties {

    private Integer port = 80042;
}
