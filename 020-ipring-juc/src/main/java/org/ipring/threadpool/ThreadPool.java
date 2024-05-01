package org.ipring.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author lgj
 * @Date 2024/4/30
 */
@Slf4j
public class ThreadPool {
    /**
     * submit提交可以等待线程执行完获取结果
     *
     * @param args
     */
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<?> submit1 = executorService.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("submit1 运行");
        });
        Future<String> submit2 = executorService.submit(() -> {
            log.info("submit2 运行 带返回");
            return "运行成了";
        });
        try {
            Object o1 = submit1.get();
            Object o2 = submit2.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
