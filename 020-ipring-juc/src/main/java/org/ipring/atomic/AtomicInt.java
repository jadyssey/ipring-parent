package org.ipring.atomic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author lgj
 * @Date 2024/5/15
 */
public class AtomicInt {
    static final AtomicInteger atomicInteger = new AtomicInteger();
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                for (int j = 0; j < 1000; j++) {
                    int value = atomicInteger.incrementAndGet();
                    System.out.println(Thread.currentThread().getName() + ": i = " + value);
                }
            });
        }
    }
}
