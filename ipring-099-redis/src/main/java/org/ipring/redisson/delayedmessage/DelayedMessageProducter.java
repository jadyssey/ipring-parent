package org.ipring.redisson.delayedmessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author lgj
 * @date 2024/5/10
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayedMessageProducter {
    private final RedissonClient redisson;

    @Scheduled(fixedRate = 1000)
    public void producter() {
        // 创建延迟队列
        RBlockingQueue<String> blockingQueue = redisson.getBlockingQueue("myDelayedQueue");
        // 延迟发送消息
        RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(blockingQueue);
        delayedQueue.offer(String.format(" [Delayed Message %s]", LocalDateTime.now()), ThreadLocalRandom.current().nextInt(10), TimeUnit.SECONDS);
    }
}
