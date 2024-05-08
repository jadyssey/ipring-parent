package org.ipring.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.constant.CommonConstants;
import org.ipring.sender.UserSender;
import org.ipring.util.JsonUtils;
import org.ipring.websocket.model.MqMsgEnum;
import org.ipring.websocket.model.WebSocketCmd;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 这个Sender目的是为了给客户端发送数据用的
 * 比如K线的实时数据, 账户多地登录时发送同步数据等
 *
 * @author: lgj
 * @date: 2024/04/03 15:45
 * @description:
 */
@ChannelHandler.Sharable
@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketPushHandler implements UserSender<WebSocketCmd> {

    public static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 这个方法是发送给所有的拥有对应账户属性值的
     */
    @Override
    public void send(WebSocketCmd data) {
        log.debug("Socket|send: data={}", data);
        // 向所有Channel发送消息
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data))).addListener((ChannelGroupFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Message sent successfully to all channels");
            } else {
                System.err.println("Failed to send message to one or more channels");
                future.cause().printStackTrace();
            }
        });
    }

    @Override
    public void send2Account(Long accountId, WebSocketCmd data) {
        log.debug("Socket|send2Account： accountId={}, data={}", accountId, data);
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data)), channel -> Optional.ofNullable(channel.attr(CommonConstants.ACC_KEY)).map(Attribute::get).map(set -> set.contains(accountId)).orElse(false));
    }

    @Override
    public void send2UserId(String token, WebSocketCmd data) {
        log.debug("Socket|send2UserId： token={}, data={}", token, data);
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data)), channel -> token.equals(channel.attr(CommonConstants.TOKEN_KEY).get()));
    }

    @Scheduled(initialDelay = 2 * 1000, fixedRate = 10 * 1000)
    public void sendData() {
        String msg = "WebsocketPushHandler， 当前时间：" + System.currentTimeMillis();
        this.send(WebSocketCmd.of(System.currentTimeMillis(), MqMsgEnum.ORDER_REFRESH, msg));
    }
}
