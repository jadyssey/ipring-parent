package org.ipring.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

        List<Future<Integer>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Future<Integer> submit = executorService.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(3);
                    return ThreadLocalRandom.current().nextInt(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("submit1 运行");
                return ThreadLocalRandom.current().nextInt(10);
            });
            list.add(submit);
        }
        List<Integer> resp = list.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return null;
            }
        }).collect(Collectors.toList());
        System.out.println("resp = " + resp);

    }
}
