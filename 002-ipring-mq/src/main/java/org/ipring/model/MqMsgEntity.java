package org.ipring.model;

import lombok.Data;
import org.ipring.util.JsonUtils;

/**
 * @author: Rainful
 * @date: 2024/04/07 10:04
 * @description:
 */
@Data
public class MqMsgEntity {

    private MqMsgEnum cmd;
    private Long sendTime;
    private String data;

    public static MqMsgEntity of(MqMsgEnum cmd, Object data) {
        final MqMsgEntity entity = new MqMsgEntity();
        entity.setCmd(cmd);
        entity.setData(JsonUtils.toJson(data));
        entity.setSendTime(System.currentTimeMillis());
        return entity;
    }

    public static MqMsgEntity refreshOrder(Object data) {
        return MqMsgEntity.of(MqMsgEnum.ORDER_REFRESH, data);
    }
}
