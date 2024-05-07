package org.ipring.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.constant.CommonConstants;
import org.ipring.sender.UserSender;
import org.ipring.util.JsonUtils;
import org.ipring.websocket.model.WebSocketCmd;
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
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data)));
    }

    @Override
    public void send2Account(Long accountId, WebSocketCmd data) {
        log.debug("Socket|send2Account： accountId={}, data={}", accountId, data);
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data)),
                channel -> Optional.ofNullable(channel.attr(CommonConstants.ACC_KEY)).map(Attribute::get).map(set -> set.contains(accountId)).orElse(false));
    }

    @Override
    public void send2UserId(String token, WebSocketCmd data) {
        log.debug("Socket|send2UserId： token={}, data={}", token, data);
        CHANNELS.writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(data)), channel -> token.equals(channel.attr(CommonConstants.TOKEN_KEY).get()));
    }
}
