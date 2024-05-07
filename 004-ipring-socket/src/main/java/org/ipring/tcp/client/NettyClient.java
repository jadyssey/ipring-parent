package org.ipring.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.tcp.NettyProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * @author: lgj
 * @date: 2024/03/20 11:13
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
public class NettyClient {
    private final ThreadPoolTaskExecutor symbolMsgThreadPool;
    private final NettyProperties properties;

    public void start() {
        connect(properties.getServer(), 0);
    }

    @PreDestroy
    public void destroy() {
        log.info("netty destroy: symbolMsg is cached to redis and the symbolMsg local cache is cleared");
    }

    public void connect(String serverAddr, int times) {
        symbolMsgThreadPool.submit(() -> {
            try {
                final Bootstrap bootstrap = new Bootstrap();
                final NioEventLoopGroup group = new NioEventLoopGroup();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ClientInitializer(symbolMsgThreadPool));

                final String[] hostAndPort = serverAddr.split(":");
                final ChannelFuture future = bootstrap.connect(hostAndPort[0], Integer.parseInt(hostAndPort[1])).sync();
                log.info("Socket|client启动成功, 链接地址{}:{}", hostAndPort[0], hostAndPort[1]);
                future.channel().closeFuture().sync();
                group.shutdownGracefully();
                // 正常来说 不应该走到这里 因为上面sync是阻塞的 这里执行重连的逻辑
                if (!Thread.currentThread().isInterrupted()) {
                    log.info("Socket|client意外断开 进入重连逻辑");
                    TimeUnit.SECONDS.sleep(5);
                    connect(serverAddr, 0);
                }
            } catch (Exception ex) {
                log.error("Socket|client链接server:{}, 出现异常", serverAddr);
                if (times >= 10) {
                    log.error("Socket|多次重试无法链接到服务端{}, 等待1小时之后进入重试", serverAddr);
                    try {
                        TimeUnit.HOURS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connect(serverAddr, 0);
                } else {
                    try {
                        TimeUnit.MINUTES.sleep(times);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    log.error("Socket|第{}次重试链接server:{}", times, serverAddr);
                    connect(serverAddr, times + 1);
                }
            }
        });
    }
}
