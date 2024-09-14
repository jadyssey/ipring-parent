package org.ipring.constant;

/**
 * @author lgj
 * @date 8/2/2023
 **/
public class RedisKey {

    public static final String LOCK_CREATE_ACCOUNT = "lock:account:create:";
    public static final String LOCK_UPDATE_ORDER = "lock:order:update:";

    /**
     * 品种的redis缓存
     */
    public static final String PREFIX_SYMBOL = "trade:symbol:";

    /**
     * 订单的redis缓存
     */
    public static final String PREFIX_ORDER = "trade:order:";

    /**
     * 品种报价缓存
     */
    public static final String SYMBOL_MSG_CACHE = "trade:symbol:msg";
}
