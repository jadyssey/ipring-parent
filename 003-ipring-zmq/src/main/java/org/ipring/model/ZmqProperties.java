package org.ipring.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: lgj
 * @date: 2024/03/19 13:47
 * @description:
 */
@Component
@ConfigurationProperties(prefix = "zmq")
@Data
public class ZmqProperties {

    private SubAddress subscribeAddress;

    private String publishOne;
    private String publishTwo;

    @Data
    public static class SubAddress {

        private String fb;
        private String localOne;
        private String localTwo;
    }
}
