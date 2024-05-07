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
public class ZmqInitializer {

    @Bean
    @Order(-1)
    public ZmqPubAbs zmqPubOne(ZmqProperties properties) {
        return new ZmqPubOne(properties);
    }

    @Bean
    @Order(-1)
    public ZmqPubAbs zmqPubTwo(ZmqProperties properties) {
        return new ZmqPubTwo(properties);
    }
}
