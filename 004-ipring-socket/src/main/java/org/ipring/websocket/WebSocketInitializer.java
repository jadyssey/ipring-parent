package org.ipring.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.websocket.config.WebsocketProperties;

import java.util.concurrent.TimeUnit;

/**
 * @author: Rainful
 * @date: 2024/04/03 15:27
 * @description:
 */
@RequiredArgsConstructor
@Slf4j
public class WebSocketInitializer extends ChannelInitializer<SocketChannel> {

    private final WebsocketProperties properties;
    private final WebsocketRecvHandler recvHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(properties.getHeartBeatCheck(), 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new WebsocketSecurityHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler(properties.getPath(), Boolean.TRUE));
        pipeline.addLast(new WebSocketKeepAliveHandler(properties));
        pipeline.addLast(recvHandler);
    }
}
