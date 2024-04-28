package org.ipring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lgj
 * @description：spring-boot 多线程  @Scheduled注解 并发定时任务
 * @date 2022-08-18
 **/

@EnableScheduling
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    public static final String EXECUTOR_SERVICE = "scheduledExecutor";
    public static final Integer EXECUTOR_CORE_POOL_SIZE = 10;

    @Bean(EXECUTOR_SERVICE)
    public ThreadPoolTaskScheduler taskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 设置线程数
        scheduler.setPoolSize(EXECUTOR_CORE_POOL_SIZE);
        // 设置等待终止秒数
        scheduler.setAwaitTerminationSeconds(60);
        // 设置拒绝策略
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置默认线程名称
        scheduler.setThreadNamePrefix("schedule-thread-pool-");
        // 等待所有任务结束后再关闭线程池
        //scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}