package org.ipring.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.util.JsonUtils;
import org.ipring.websocket.config.WebsocketProperties;
import org.ipring.websocket.model.WebSocketCmd;
import org.springframework.util.StringUtils;

/**
 * @author: Rainful
 * @date: 2024/04/03 15:54
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketKeepAliveHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final WebsocketProperties properties;

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean superCheck = super.acceptInboundMessage(msg);
        if (!superCheck) return false;

        final String text = ((TextWebSocketFrame) msg).text();
        if (!StringUtils.hasText(text)) return false;
        final WebSocketCmd webSocketCmd = JsonUtils.toObject(text, WebSocketCmd.class);
        return null != webSocketCmd && null != webSocketCmd.getCmd() && 100 == webSocketCmd.getCmd();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        ctx.channel().writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(WebSocketCmd.hb())));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent event = (IdleStateEvent) evt;
            if (IdleState.READER_IDLE == event.state()) {
                log.error("Socket|断开连接|心跳超时: ip={}", ctx.channel().remoteAddress());
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("Socket|关闭链接：ip={}", ctx.channel().remoteAddress());
        ctx.close();
    }
}
