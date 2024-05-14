package org.ipring.client.two;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Rainful
 * @date: 2024/03/19 13:47
 * @description:
 */
@Component
@ConfigurationProperties(prefix = "my.zmq")
@Data
public class MyZmqProperties {
    private String subOne;
    private String subTwo;
}
