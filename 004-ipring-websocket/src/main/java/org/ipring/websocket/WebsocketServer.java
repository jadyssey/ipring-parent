package org.ipring.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.websocket.config.WebsocketProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * @author: Rainful
 * @date: 2024/04/03 15:24
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
@Order(10)
public class WebsocketServer implements ApplicationRunner {

    private final WebsocketProperties properties;
    private final WebsocketRecvHandler recvHandler;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        final ServerBootstrap server = new ServerBootstrap();
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss"));
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));

        try {

            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, properties.getLimitCon())
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new WebSocketInitializer(properties, recvHandler));

            final ChannelFuture future = server.bind(properties.getPort()).sync();
            future.channel().closeFuture()
                    .addListener((ChannelFutureListener) future1 -> {
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                        log.info("Socket|链路关闭: channel={}", future1.channel().toString());
                    });
            log.info("Socket|server启动完毕: 端口:{}", properties.getPort());
        } catch (Exception ex) {
            log.error("Socket|server启动异常: e={}", ex.getMessage());
            Runtime.getRuntime().exit(-1);
        }
    }
}
