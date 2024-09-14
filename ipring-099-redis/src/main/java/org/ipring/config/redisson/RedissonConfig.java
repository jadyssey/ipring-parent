package org.ipring.config.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * 配置 Redission 锁
 *
 * @author YuanWenkai
 * @date 2020/6/23
 */
@Slf4j
@Configuration
@ConditionalOnClass(RedissonClient.class)
public class RedissonConfig {

    @Autowired
    @Lazy
    private RedisProperties redisProperties;

    /**
     * 单机模式使用
     */
    @Lazy
    @Bean
    public RedissonClient redissonClient() {
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        if (null == sentinel) {
            return null;
        }
        String prefix = "redis://";

        Config config = new Config();
        config.useSentinelServers()
                .setScanInterval(2000)
                //只从主节点读写，避免出现redis分布式锁会出现的锁丢失问题
                .setReadMode(ReadMode.MASTER)
                .setMasterName(sentinel.getMaster())
                .addSentinelAddress(nodesAsArray(sentinel.getNodes(), prefix))
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword());
        RedissonClient client = Redisson.create(config);
        log.info("Redisson 配置初始化成功");
        return client;
    }

    /**
     * 将nodes转换成array格式
     *
     * @param prefix
     * @return
     */
    public String[] nodesAsArray(List<String> nodes, String prefix) {
        String[] nodeList = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            String node = prefix + nodes.get(i);
            nodeList[i] = node;
        }
        return nodeList;
    }
}
