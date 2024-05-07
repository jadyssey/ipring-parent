package org.ipring.pub.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Author lgj
 * @Date 2024/5/7
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class SenderSchedule {
    private final Sender sender;
    @Scheduled(initialDelay = 2 * 1000, fixedRate = 10 * 1000)
    public void sendData() {
        String msg = "一条小小的消息， 当前时间：" + new Date();
        sender.send(msg);
    }
}
