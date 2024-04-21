package org.ipring.constant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * 线程池配置
 *
 * @author lgj
 * @date 8/2/2023
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ipring.task-pool")
public class TaskThreadPoolConfig {
    private int coreSize;

    private int maxSize;

    private int keepAlive;

    private int queueCapacity;
}