package org.ipring.tcp.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.sender.TickSender;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author: Rainful
 * @date: 2024/03/20 11:04
 * @description:
 */
@Slf4j
@ChannelHandler.Sharable
public class TickSenderHandler extends ChannelInboundHandlerAdapter implements TickSender<String> {

    public static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CHANNELS.add(ctx.channel());
    }

    @Override
    public void send(String data) {
        log.debug("发送数据: data={}", data);
        CHANNELS.writeAndFlush(data);
    }

    @Scheduled(initialDelay = 2 * 1000, fixedRate = 10 * 1000)
    public void sendData() {
        String msg = "tcp的方式实现长连接， 当前时间：" + System.currentTimeMillis();
        this.send(msg);
    }
}
