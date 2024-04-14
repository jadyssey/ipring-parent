package org.ipring.constant;

import java.time.Duration;

/**
 * @author lgj
 * @date 2024/4/3
 **/
public class ExpireConstant {
    /**
     * 品种缓存过期时间
     */
    public static final Duration SYMBOL_EXPIRE = Duration.ofDays(1);

    /**
     * 订单缓存过期时间 1 天
     */
    public static final Duration ORDER_EXPIRE = Duration.ofDays(1);
}
