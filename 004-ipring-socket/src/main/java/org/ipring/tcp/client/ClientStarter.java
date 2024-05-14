package org.ipring.tcp.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.tcp.NettyProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author: lgj
 * @date: 2024/03/20 10:33
 * @description:
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NettyProperties.class)
@Import({NettyClient.class})
@ConditionalOnProperty(value = "tcp.enable", havingValue = "true")
public class ClientStarter implements ApplicationRunner {

    private final NettyClient client;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        client.start();
    }
}
