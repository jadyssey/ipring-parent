package org.ipring.mq;

import org.ipring.model.entity.mq.MqMsgEntity;

/**
 * @author: Rainful
 * @date: 2024/04/07 10:11
 * @description:
 */
public interface MqSender {

    void send(String topic, Object key, MqMsgEntity msg);

    void asyncSend(String topic, MqMsgEntity msg);
}
