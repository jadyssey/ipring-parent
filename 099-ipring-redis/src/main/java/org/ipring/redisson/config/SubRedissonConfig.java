package org.ipring.redisson.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * @author lgj
 * @date 2024/5/10
 **/
@Configuration
public class SubRedissonConfig {

    @Bean
    public RedissonClient redisson() {
        // 创建 Redisson 客户端连接到 Redis 服务器
        List<String> addrList = Arrays.asList("192.168.7.67:26379", "192.168.7.68:26379", "192.168.7.69:26379");
        Config config = new Config();
        config.useSentinelServers()
                .setScanInterval(2000)
                //只从主节点读写，避免出现redis分布式锁会出现的锁丢失问题
                .setReadMode(ReadMode.MASTER)
                .setMasterName("mymaster")
                .addSentinelAddress(nodesAsArray(addrList, "redis://"))
                .setDatabase(66)
                .setPassword("Aa123456");
        return Redisson.create(config);
    }

    public static String[] nodesAsArray(List<String> nodes, String prefix) {
        String[] nodeList = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            String node = prefix + nodes.get(i);
            nodeList[i] = node;
        }
        return nodeList;
    }
}
