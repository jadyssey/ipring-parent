package org.ipring.zmq.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Rainful
 * @date: 2024/03/19 13:47
 * @description:
 */
@Component
@ConfigurationProperties(prefix = "zmq")
@Data
public class ZmqProperties {

    private SubAddress subscribeAddress;

    @Data
    public static class SubAddress {

        private String fb;
    }
}
