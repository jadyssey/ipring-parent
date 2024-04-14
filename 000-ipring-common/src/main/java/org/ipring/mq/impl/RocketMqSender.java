package org.ipring.mq.impl;

import org.ipring.model.entity.mq.MqMsgEntity;
import org.ipring.mq.MqSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

/**
 * @author: Rainful
 * @date: 2024/04/08 14:59
 * @description:
 */
@RequiredArgsConstructor
@Slf4j
public class RocketMqSender implements MqSender {

    private final RocketMQTemplate template;

    @Override
    public void send(String topic, Object key, MqMsgEntity msg) {
        log.info("mq sender topic:{}, key:{}, msg:{}", topic, key, msg);
        template.syncSendOrderly(topic, msg, String.valueOf(key));
    }

    @Override
    public void asyncSend(String topic, MqMsgEntity msg) {
        // todo 优化
        template.asyncSend(topic, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                // 消息发送成功时调用
                System.out.println("Message sent successfully: " + sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                // 消息发送失败时调用
                System.err.println("Message sending failed: " + throwable.getMessage());
            }
        });
    }
}
