package org.ipring.config.redisson;

import org.ipring.util.RedissonUtil;

import java.lang.annotation.*;

/**
 * 用于标记加锁方法, 使用 Redission Lock
 *
 * @author lgj
 * @see LockAspect
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {

    /**
     * @return Redisson Lock 键名前缀
     */
    String value();

    /**
     * @return Redisson Lock 键名后缀, 会拼上前缀, 可以使用 SpEL 表达式
     * <p>
     * Example: {@code #model.id + ':' + {#model.type ?: 'default'}}
     */
    String suffix() default "";

    /**
     * @return 最大等待时间
     */
    int waitTime();

    /**
     * @return 最大持有时间, 到期自动解锁
     */
    int leaseTime() default RedissonUtil.DEFAULT_LEASE_TIME;
}
