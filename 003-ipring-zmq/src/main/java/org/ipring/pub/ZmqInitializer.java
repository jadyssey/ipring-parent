package org.ipring.pub;

import org.ipring.model.ZmqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author: lgj
 * @date: 2024/03/20 16:14
 * @description:
 */
@Configuration
@ConditionalOnProperty(value = "netty.mode", havingValue = "CLIENT")
public class ZmqInitializer {

    @Bean
    @Order(-1)
    public ZmqPubAbs zmqPubUser(ZmqProperties properties) {
        return new ZmqPubOne(properties);
    }

    @Bean
    @Order(-1)
    public ZmqPubAbs zmqPubVip(ZmqProperties properties) {
        return new ZmqPubTwo(properties);
    }
}
