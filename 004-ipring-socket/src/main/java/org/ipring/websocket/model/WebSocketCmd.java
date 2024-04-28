package org.ipring.websocket.model;

import lombok.Data;

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
        cmd.setTime(System.currentTimeMillis());
        return cmd;
    }

    public static WebSocketCmd of(Long now, MqMsgEnum mqCmd, Object data) {
        final WebSocketCmd cmd = new WebSocketCmd();
        cmd.setCmd(mqCmd.getType());
        cmd.setData(data);
        cmd.setTime(now);
        return cmd;
    }

    // 暂时这么写 又不是不能用
    private static final WebSocketCmd HB_INSTANCE = new WebSocketCmd() {
        {
            setCmd(100);
            setData("pong");
        }
    };

    public static WebSocketCmd hb() {
        HB_INSTANCE.setTime(System.currentTimeMillis());
        return HB_INSTANCE;
    }
}
