package org.ipring.config;

import lombok.extern.slf4j.Slf4j;
import org.ipring.constant.properties.TaskThreadPoolConfig;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * Async线程池配置
 *
 * @author lgj
 * @date 13/2/2023
 **/
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    @Resource
    private TaskThreadPoolConfig taskThreadPoolConfig;

    /**
     * 通用异步任务线程池
     */
    @Bean("commonThreadPool")
    public ThreadPoolTaskExecutor commonThreadPool() {
        return executor("common-threadpool-", false);
    }

    public ThreadPoolTaskExecutor executor(String threadNamePrefix, boolean shotdownDelay) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(taskThreadPoolConfig.getCoreSize());
        //配置最大线程数
        executor.setMaxPoolSize(taskThreadPoolConfig.getMaxSize());
        //配置队列大小
        executor.setQueueCapacity(taskThreadPoolConfig.getQueueCapacity());
        //线程池维护线程所允许的空闲时间
        executor.setKeepAliveSeconds(taskThreadPoolConfig.getKeepAlive());
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        //设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
        executor.setWaitForTasksToCompleteOnShutdown(shotdownDelay);
        //设置线程池中任务的等待时间，如果超过这个时候还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住
        executor.setAwaitTerminationSeconds(30);
        // 增加 CustomTaskDecorator 属性的配置
        // executor.setTaskDecorator(new CustomTaskDecorator());

        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("[Catch ThreadPoolException={}] errorMessage={}", method, ex);
    }
}