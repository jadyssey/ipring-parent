package org.ipring.tcp.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

/**
 * @author: Rainful
 * @date: 2024/03/20 14:12
 * @description:
 */
public class ClientHeartBeatHandler extends ChannelInboundHandlerAdapter {

    private ScheduledFuture<?> heartBeatFu = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        heartBeatFu = ctx.channel().eventLoop().scheduleAtFixedRate(() -> ctx.channel().writeAndFlush(TickConstant.HEARTBEAT), 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (heartBeatFu == null) return;
        heartBeatFu.cancel(true);
    }
}
