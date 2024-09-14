package org.ipring;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.ipring.api.DemoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Consumer implements CommandLineRunner {
    @DubboReference(url = "dubbo://192.168.121.144:28016")
    private DemoService demoService;

    @Override
    public void run(String... args) throws Exception {
        String result = demoService.sayHello("world");
        log.info("Receive result ======> {}", result);
    }
}