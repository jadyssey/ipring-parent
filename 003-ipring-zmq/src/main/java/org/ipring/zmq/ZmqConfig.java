package org.ipring.zmq;

import lombok.RequiredArgsConstructor;
import org.ipring.zmq.model.ZmqClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/03 17:14
 * @description:
 */
@Configuration
@Import(FbZmqClient.class)
@RequiredArgsConstructor
@Order(value = 11)
public class ZmqConfig implements ApplicationRunner {

    private final List<ZmqClient> clients;

    @Override
    @Async("commonThreadPool")
    public void run(ApplicationArguments args) throws Exception {
        clients.forEach(ZmqClient::run);
    }
}
