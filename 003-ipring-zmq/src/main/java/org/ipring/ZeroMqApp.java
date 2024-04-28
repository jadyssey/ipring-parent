package org.ipring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author lgj
 * @date 2024/4/19
 **/

@SpringBootApplication
public class ZeroMqApp {

    public static void main(String[] args) {
        SpringApplication.run(ZeroMqApp.class, args);
    }
}
