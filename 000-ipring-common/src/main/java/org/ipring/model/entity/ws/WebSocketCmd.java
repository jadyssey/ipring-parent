package org.ipring.model.entity.ws;

import org.ipring.enums.mq.MqMsgEnum;
import org.ipring.model.entity.order.OrderTickDTO;
import lombok.Data;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/03 16:35
 * @description:
 */
@Data
public class WebSocketCmd {

    /**
     * 指令
     */
    private Integer cmd;

    private Long time;

    /**
     * 数据
     */
    private Object data;

    public static WebSocketCmd account(MqMsgEnum mqCmd, Object data) {
        final WebSocketCmd cmd = new WebSocketCmd();
        cmd.setCmd(mqCmd.getType());
        cmd.setData(data);
        return cmd;
    }

    public static WebSocketCmd order(long now, OrderTickDTO order) {
        return of(now, MqMsgEnum.ORDER_REFRESH, order);
    }

    public static WebSocketCmd profit(long now, List<OrderTickDTO> list) {
        return of(now, MqMsgEnum.ORDER_PROFIT, list);
    }

    private static WebSocketCmd of(Long now, MqMsgEnum mqCmd, Object data) {
        final WebSocketCmd cmd = new WebSocketCmd();
        cmd.setCmd(mqCmd.getType());
        cmd.setData(data);
        cmd.setTime(now);
        return cmd;
    }
}
