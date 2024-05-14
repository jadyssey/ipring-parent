package org.ipring.tcp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author: lgj
 * @date: 2024/03/20 14:18
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
public class ChannelReadHandler extends SimpleChannelInboundHandler<String> {

    private final ThreadPoolTaskExecutor symbolMsgThreadPool;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        symbolMsgThreadPool.execute(() -> {
            log.info("client 收到消息， msg = “{}” ", msg);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Socket|链接服务端发生异常:", cause);
        ctx.close();
    }
}
