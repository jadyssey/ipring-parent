package org.ipring.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.constant.CommonConstants;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.util.JsonUtils;
import org.ipring.util.MessageUtils;
import org.ipring.websocket.model.MqMsgEnum;
import org.ipring.websocket.model.WebSocketCmd;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: lgj
 * @date: 2024/04/03 15:45
 * @description:
 */
@ChannelHandler.Sharable
@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketRecvHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean superCheck = super.acceptInboundMessage(msg);
        if (!superCheck) return false;

        final String text = ((TextWebSocketFrame) msg).text();
        if (!StringUtils.hasText(text)) return false;
        final WebSocketCmd webSocketCmd = JsonUtils.toObject(text, WebSocketCmd.class);
        return null != webSocketCmd && null != webSocketCmd.getCmd() && 200 == webSocketCmd.getCmd();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String token = Optional.ofNullable(ctx.channel().attr(CommonConstants.TOKEN_KEY)).map(Attribute::get).orElse("token is null");
        final WebSocketCmd cmd = JsonUtils.toObject(msg.text(), WebSocketCmd.class);
        if (null == cmd) {
            log.info("Socket|cmd无法解析: token={}, text={}", token, msg.text());
            ctx.channel().attr(CommonConstants.ACC_KEY).set(Collections.emptySet());
            return;
        }

        final List<?> data = (List<?>) cmd.getData();
        if (CollectionUtils.isEmpty(data)) {
            log.info("Socket|cmd取消订阅：token={}，accIds:{}", token, ctx.channel().attr(CommonConstants.ACC_KEY).get());
            ctx.channel().attr(CommonConstants.ACC_KEY).set(Collections.emptySet());
            return;
        }
        final Set<Long> accIds = data.stream().map(ele -> ((Number) ele).longValue()).collect(Collectors.toSet());
        log.info("Socket|cmd更新订阅: token={}, accIds:{}", token, accIds);

        if (CollectionUtils.isEmpty(accIds)) return;
        ctx.channel().attr(CommonConstants.ACC_KEY).set(accIds);
        WebSocketCmd of = WebSocketCmd.of(System.currentTimeMillis(), MqMsgEnum.ACCOUNT_BALANCE, "账号订阅成功" + accIds);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(JsonUtils.toJson(of)));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String token = Optional.ofNullable(ctx.channel().attr(CommonConstants.TOKEN_KEY)).map(Attribute::get).orElse("token is null");
        log.error("Socket|异常发生: token={}", token, cause);
        ctx.channel().writeAndFlush(MessageUtils.getMsg(SystemServiceCode.SystemApi.PARAM_ERROR.getI18nKey()));
        ctx.close();
    }
}
