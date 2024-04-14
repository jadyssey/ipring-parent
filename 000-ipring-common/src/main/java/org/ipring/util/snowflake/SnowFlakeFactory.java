package org.ipring.util.snowflake;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 雪花算法工厂
 *
 * @author lgj
 * @date 2024/4/3
 **/
@Slf4j
public class SnowFlakeFactory {
    /**
     * 默认的机器id
     */
    private static final long DEFAULT_MACHINE_ID = 1;

    private static final String ORDER_SNOW_FLAKE = "order_snow_flake";
    private static final String ACCOUNT_SNOW_FLAKE = "account_snow_flake";

    /**
     * 缓存SnowFlake对象
     */
    private static ConcurrentMap<String, AbstractSnowFlake> snowFlakeCache = new ConcurrentHashMap<>(2);

    /**
     * 获取订单雪花算法对象
     *
     * @return
     */
    public static OrderSnowFlake getOrderCache() {
        return (OrderSnowFlake) getSnowFlakeInstance(ORDER_SNOW_FLAKE);
    }

    /**
     * 获取账号雪花算法对象
     *
     * @return
     */
    public static AccountSnowFlake getAccountCache() {
        return (AccountSnowFlake) getSnowFlakeInstance(ACCOUNT_SNOW_FLAKE);
    }


    /**
     * 获取雪花算法实例的方法
     *
     * @param key
     * @return
     */
    private static AbstractSnowFlake getSnowFlakeInstance(String key) {
        // 在缓存中查找指定键的雪花算法实例
        return snowFlakeCache.computeIfAbsent(key, k -> {
            // 根据键的值创建相应的子类实例
            switch (k) {
                case ORDER_SNOW_FLAKE:
                    return new OrderSnowFlake(generateMachineID());
                case ACCOUNT_SNOW_FLAKE:
                    return new AccountSnowFlake(generateMachineID());
                default:
                    throw new IllegalArgumentException("snowflake Invalid key: " + k);
            }
        });
    }
    

    /**
     * 根据ip地址最后一个字节生成机器id
     *
     * @return
     */
    public static long generateMachineID() {
        try {
            // 获取本地主机地址
            InetAddress inetAddress = InetAddress.getLocalHost();
            byte[] ipAddressBytes = inetAddress.getAddress();

            // 仅使用IP地址的最后一部分（例如IPv4的最后一个字节）
            int lastByte = ipAddressBytes[ipAddressBytes.length - 1] & 0xFF;

            // 将IP地址的最后一个字节映射到Snowflake算法的机器ID范围内
            // 通常机器ID的范围为0到31
            int machineIdRange = 31;
            return lastByte % (machineIdRange + 1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return DEFAULT_MACHINE_ID;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println("orderId = " + getOrderCache().nextId());
            System.out.println("accountId = " + getAccountCache().nextId());
        }
    }
}
