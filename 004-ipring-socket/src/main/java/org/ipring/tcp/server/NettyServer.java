package org.ipring.tcp.server;

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
import org.ipring.tcp.server.sender.TickSenderHandler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * @author: lgj
 * @date: 2024/03/19 18:17
 * @description:
 */
@Order(10)
@Slf4j
@RequiredArgsConstructor
public class NettyServer implements ApplicationRunner {

    private final TickSenderHandler sender;
    private final TcpServerProperties properties;

    @Override
    public void run(ApplicationArguments args) {

        final ServerBootstrap server = new ServerBootstrap();
        // netty是基于reactor网络模型的 所以按理来说应该是一个boss多个worker
        // 但是呢 考虑到这个主要任务量是io层面 其实不会有多少链接 基于下一层的tick mobile 所以这里只给到一个group够用了
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));

        try {

            server.group(workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 128) // 同理 不需要设置很高
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ServerInitializer(sender));

            final ChannelFuture future = server.bind(properties.getPort()).sync();
            future.channel().closeFuture()
                    .addListener((ChannelFutureListener) future1 -> {
                        workerGroup.shutdownGracefully();
                        log.info("Socket|链路关闭：{}", future1.channel().toString());
                    });
            log.info("Socket|server启动完毕: 端口:{}", properties.getPort());
        } catch (Exception ex) {
            log.error("Socket|server启动异常: {}", ex.getMessage());
            Runtime.getRuntime().exit(-1);
        }
    }
}
