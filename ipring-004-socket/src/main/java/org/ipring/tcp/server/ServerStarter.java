package org.ipring.tcp.server;

import lombok.RequiredArgsConstructor;
import org.ipring.tcp.server.sender.TickSenderHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author: lgj
 * @date: 2024/03/19 13:59
 * @description:
 */
@Configuration
@RequiredArgsConstructor
@Import({NettyServer.class, TcpServerProperties.class, TickSenderHandler.class})
@ConditionalOnProperty(value = "tcp.enable", havingValue = "true")
public class ServerStarter {
}
