package org.ipring.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;
import org.ipring.constant.AuthConstant;
import org.ipring.constant.CommonConstants;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.util.HttpUtils;
import org.ipring.util.MessageUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author: lgj
 * @date: 2024/04/03 16:42
 * @description:
 */
@Slf4j
public class WebsocketSecurityHandler extends SimpleChannelInboundHandler<HttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        final HttpHeaders headers = msg.headers();
        String token = headers.get(AuthConstant.TOKEN);
        if (!StringUtils.hasText(token)) {
            // 兼容web无法设置websocket请求头
            final String uri = msg.uri();
            final QueryStringDecoder qs = new QueryStringDecoder(uri, StandardCharsets.UTF_8, true);
            final Map<String, List<String>> parameters = qs.parameters();
            final List<String> tokenList = parameters.get(AuthConstant.TOKEN);
            if (CollectionUtils.isEmpty(tokenList)) {
                log.error("Socket|未找到请求头: ip={}", ctx.channel().remoteAddress());
                ctx.channel().writeAndFlush(MessageUtils.getMsg(SystemServiceCode.SystemApi.PARAM_ERROR.getI18nKey()));
                ctx.channel().close();
                return;
            }
            token = tokenList.get(0);
        }
        ctx.channel().attr(CommonConstants.ACC_KEY).set(Collections.emptySet());
        ctx.channel().attr(CommonConstants.TOKEN_KEY).set(token);
        ctx.channel().pipeline().remove(this);
        ctx.fireChannelRead(msg);
    }
}
