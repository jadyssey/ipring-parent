package org.ipring.mq.impl;

import org.ipring.model.entity.mq.MqMsgEntity;
import org.ipring.mq.MqSender;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Rainful
 * @date: 2024/04/07 11:39
 * @description:
 */
@Slf4j
public class LogMqSender implements MqSender {

    @Override
    public void send(String topic, Object key, MqMsgEntity msg) {
        log.info("mq sender topic:{}, key:{}, msg:{}", topic, key, msg);
    }

    @Override
    public void asyncSend(String topic, MqMsgEntity msg) {
        log.info("mq asyncSend topic:{}, key:{}, msg:{}", topic, msg);
    }
}
