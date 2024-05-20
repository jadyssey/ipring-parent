package org.ipring.util;

import org.ipring.model.common.Return;
import org.ipring.model.common.ReturnFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author lgj
 */
@Slf4j
@Component
public class RedissonUtil {
    public static final int DEFAULT_LEASE_TIME = 30;
    public static final int ZERO = 0;
    public static final int ONE_SECOND = 1;
    public static final int TEN_SECOND = 10;
    public static final int ONE_MINUTE = 60;
    public static final int TWO_MINUTE = 2 * ONE_MINUTE;

    @Lazy
    @Autowired
    private RedissonClient redissonClient;


    /**
     * 加锁
     *
     * @param lockKey
     * @return
     */
    public RLock lock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 公平锁
     *
     * @param key
     * @return
     */
    public RLock fairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    /**
     * 带超时的锁
     *
     * @param lockKey
     * @param timeout 超时时间 单位：秒
     */
    public RLock lock(String lockKey, int timeout) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(timeout, TimeUnit.SECONDS);
        return lock;
    }

    /**
     * 读写锁
     *
     * @param key
     * @return
     */
    public RReadWriteLock readWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    /**
     * 加锁
     *
     * @param key
     * @param supplier
     * @return
     */
    public <T> T lock(String key, Supplier<T> supplier) {
        RLock lock = lock(key);
        try {
            lock.lock();
            return supplier.get();
        } finally {
            unlock(lock);
        }
    }

    /**
     * 常用于controller中防止前端重复请求
     *
     * @param key
     * @param supplier
     * @return
     */
    public Return<?> tryAndLock(String key, Supplier<Return<?>> supplier) {
        if (!this.tryLock(key, ONE_SECOND, ONE_MINUTE)) {
            return ReturnFactory.frequently();
        }
        try {
            return supplier.get();
        } finally {
            this.unlock(key);
        }
    }

    public void tryAndLock(String key, int timeout, Runnable runnable) {
        if (!this.tryLock(key, ONE_SECOND, timeout)) {
            return;
        }
        try {
            runnable.run();
        } finally {
            this.unlock(key);
        }
    }

    /**
     * 尝试加锁，快速失败
     *
     * @param key
     * @param waitTime 0
     * @param timeout  30
     * @param runnable
     * @return
     */
    public boolean lockByFastFail(String key, Runnable runnable) {
        if (!this.tryLock(key, 0, DEFAULT_LEASE_TIME)) {
            return false;
        }
        try {
            runnable.run();
            return true;
        } finally {
            this.unlock(key);
        }
    }

    public void lock(String key, Runnable runnable) {
        RLock lock = lock(key);
        try {
            lock.lock();
            runnable.run();
        } finally {
            unlock(lock);
        }
    }

    /**
     * 加锁
     *
     * @param key
     * @param supplier
     * @return
     */
    public <T> T lock(String key, int timeout, Supplier<T> supplier) {
        RLock lock = lock(key);
        try {
            lock.lock(timeout, TimeUnit.SECONDS);
            return supplier.get();
        } finally {
            unlock(lock);
        }
    }


    /**
     * 尝试获取锁
     *
     * @param lockKey
     * @param waitTime  等待时间
     * @param leaseTime 自动释放锁时间
     * @return
     */
    public boolean tryLock(String lockKey, int waitTime, int leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.unlock();
    }


    /**
     * 释放锁
     *
     * @param lock
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isLocked()) {
            lock.unlock();
        }
    }


    static boolean print(Runnable runnable) {
        if (false) return false;
        try {
            runnable.run();
            return true;
        } finally {
            System.out.println("finally");
        }
    }

    public static void main(String[] args) {
        boolean print = print(() -> {
            System.out.println("1 / 0 = " + 1 / 0);
        });
        System.out.println("print = " + print);
    }

}