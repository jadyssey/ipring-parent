package org.ipring.cache.symbol;

import java.util.Map;
import java.util.Optional;

/**
 * @author lgj
 * @date 2024/4/7
 **/
public abstract class CacheLocalService<T> {

    /**
     * 添加元素
     *
     * @param key
     * @param o
     */
    public abstract void put(String key, T o);

    /**
     * 获取元素
     *
     * @param marketType
     * @param symbolId
     * @return
     */
    public abstract Optional<T> get(Integer marketType, String symbolId);

    /**
     * 获取整个本地缓存对象
     *
     * @return
     */
    public abstract Map<String, T> getHolder();

    /**
     * 清除整个本地缓存
     */
    public abstract void clear();
}
