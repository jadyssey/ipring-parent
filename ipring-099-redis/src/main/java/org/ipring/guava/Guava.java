package org.ipring.guava;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author lgj
 * @date 2024/5/15
 **/
public class Guava {
    private static final LoadingCache<String, String> cache =
            CacheBuilder.newBuilder()
                    .initialCapacity(4)
                    .maximumSize(8)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .refreshAfterWrite(5, TimeUnit.SECONDS)
                    .build(CacheLoader.asyncReloading(new CacheLoader<String, String>() {
                        @Override
                        public String load(String s) {
                            return "null";
                        }
                    }, Executors.newFixedThreadPool(3)));

    public static void main(String[] args) throws InterruptedException {
        System.out.println(cache.getUnchecked("Guava")); // 输出：Hello, Guava
        //for (int i = 0; i < 100; i++) {
        //    TimeUnit.SECONDS.sleep(6);
        //}
        TimeUnit.SECONDS.sleep(6);
        cache.getUnchecked("Guava");
    }
}
