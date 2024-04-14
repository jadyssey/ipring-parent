package org.ipring.mq;

import org.ipring.mq.impl.LogMqSender;
import org.ipring.mq.impl.RocketMqSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

/**
 * @author: Rainful
 * @date: 2024/04/07 19:30
 * @description:
 */
@Configuration
@Import(TopicProperties.class)
@Slf4j
public class MqConfig {

    @Lazy
    @Bean
    @Order(1)
    @ConditionalOnClass(RocketMQTemplate.class)
    @ConditionalOnProperty(value = "mq.enable", havingValue = "yes", matchIfMissing = true)
    public MqSender rocketMqSender(RocketMQTemplate template) {
        log.info("rocket mq 初始化");
        return new RocketMqSender(template);
    }

    @Bean
    @ConditionalOnMissingBean(MqSender.class)
    public MqSender logMqSender() {
        log.info("local log mq 初始化");
        return new LogMqSender();
    }
}
