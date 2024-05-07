package org.ipring.rocket;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.ipring.model.MqMsgEntity;
import org.ipring.model.MqMsgEnum;
import org.ipring.model.TradeOrderEntity;
import org.ipring.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author: lgj
 * @date: 2024/04/07 10:54
 * @description:
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${topic.order}", consumerGroup = "niu_group", selectorExpression = "niu",
        messageModel = MessageModel.BROADCASTING)
public class NiuOrderRocketMqListener implements RocketMQListener<String> {

    @Override
    public void onMessage(String s) {
        MqMsgEntity mqMsg = JsonUtils.toObject(s, MqMsgEntity.class);
        if (Objects.isNull(mqMsg) || StringUtils.isBlank(mqMsg.getData())) return;
        if (!MqMsgEnum.ORDER_REFRESH.equals(mqMsg.getCmd())) return;
        log.info("MQ消息|耗时：消息类型={}, RT={}ms", mqMsg.getCmd(), System.currentTimeMillis() - mqMsg.getSendTime());
        TradeOrderEntity entity = JsonUtils.toObject(mqMsg.getData(), TradeOrderEntity.class);
        if (Objects.isNull(entity) || !Arrays.asList(0, 1, 6).contains(entity.getDealReason())) return;
        log.info("模型：{}", entity);
        // todo
        // entity
    }
}
