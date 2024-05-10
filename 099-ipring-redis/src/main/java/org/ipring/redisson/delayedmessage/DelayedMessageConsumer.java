package org.ipring.redisson.delayedmessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author lgj
 * @date 2024/5/10
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayedMessageConsumer {
    private final RedissonClient redisson;
    private final ThreadPoolTaskExecutor commonThreadPool;

    @PostConstruct
    public void consumer() {
        log.info("开始监听延迟消息");
        // 创建延迟队列
        RBlockingQueue<String> blockingQueue = redisson.getBlockingQueue("myDelayedQueue");

        // 监听延迟队列
        commonThreadPool.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从延迟队列中取出消息
                    String message = blockingQueue.take();
                    // 处理延迟消息
                    log.info("Received message: " + message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
