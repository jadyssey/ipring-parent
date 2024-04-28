package org.ipring.tcp.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Rainful
 * @date: 2024/03/20 10:57
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
public class ServerKeepAliveHandler extends ChannelDuplexHandler {

    // private final NoticeService noticeService;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent event = (IdleStateEvent) evt;
            if (IdleState.READER_IDLE == event.state()) {
                //String msg = String.format("长时间未收到客户端:%s 心跳, 断开连接", ctx.channel().remoteAddress());
                log.error("Socket|断开连接|心跳超时: ip={}", ctx.channel().remoteAddress());
                // noticeService.sendNotice(msg);
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
