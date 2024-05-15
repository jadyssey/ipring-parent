package org.ipring.client.two;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MyZmqConfig implements ApplicationRunner {
    private final List<MyZmqClient> clients;

    @Override
    public void run(ApplicationArguments args) {
        clients.forEach(MyZmqClient::run);
    }
}