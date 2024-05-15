package org.ipring.guava;

import com.google.common.cache.*;

import java.util.concurrent.*;

public class CacheWrapper {

    public static <K, V> LoadingCache<K, V> createCache(
            int initialCapacity,
            int maximumSize,
            long expireAfterWriteDuration,
            long refreshAfterWriteDuration,
            Loader<K, V> loader) {

        return CacheBuilder.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWriteDuration, TimeUnit.SECONDS)
                .refreshAfterWrite(refreshAfterWriteDuration, TimeUnit.SECONDS)
                .build(CacheLoader.asyncReloading(new CacheLoader<K, V>() {
                    @Override
                    public V load(K key) {
                        return loader.load(key);
                    }
                }, Executors.newFixedThreadPool(3)));
    }

    public static void main(String[] args) {
        LoadingCache<String, String> cache = createCache(
                4,
                8,
                10,
                5,
                // 使用Lambda表达式实现CacheLoader的load方法
                key -> "null"
        );
    }
}
