package org.ipring.util;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author: Rainful
 * @date: 2024/04/02 16:15
 * @description:
 */
@Component
// @RequiredArgsConstructor
public class RedisUtil {


    @Lazy
    @Autowired
    private StringRedisTemplate template;

    //缓存空数据的时长，单位秒
    public static final Long CACHE_NULL_TTL = 60L;
    //缓存的空数据
    public static final String EMPTY_VALUE = "";

    public boolean del(String key) {
        return Boolean.TRUE.equals(template.delete(key));
    }

    public Long del(Collection<String> keys) {
        return template.delete(keys);
    }

    public String get(String key) {
        if (!StringUtils.hasText(key)) return null;
        return template.opsForValue().get(key);
    }

    public String getAndExpire(String key, Duration duration) {
        // lua start
        // set = template.opsForZSet().rangeByScore()
        // if set == null return null;
        //
        // template.opsForZSet().removeRangeByScore()
        // for set -> +time
        // load common_set = set;
        // lua end

        // mysql update
        // template.opsForZSet().removeRangeByScore()

        // template.opsForZSet().

        // todo lua
        return null;
    }

    public <T> T get(String key, Class<T> tClass) {
        final String ret = get(key);
        if (!StringUtils.hasText(ret)) return null;
        return JsonUtils.toObject(ret, tClass);
    }

    public void set(String key, Object val, Duration duration) {
        if (!StringUtils.hasText(key) || Objects.isNull(val)) return;
        template.opsForValue().set(key, JsonUtils.toJson(val), duration);
    }

    public void hAdd(String key, Object field, Object val) {
        if (!StringUtils.hasText(key) || Objects.isNull(field) || Objects.isNull(val)) return;
        template.opsForHash().put(key, field, val);
    }

    public void hDel(String key, Object... fields) {
        if (!StringUtils.hasText(key)) return;
        template.opsForHash().delete(key, fields);
    }


    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public Object hGet(final String key, final Object hKey) {
        return template.<String, Object>opsForHash().get(key, hKey);
    }

    /**
     * 批量添加hashmap，无过期时间
     *
     * @param key
     * @param map
     */
    public void hPutAll(final String key, Map<?, ?> map) {
        template.opsForHash().putAll(key, map);
    }

    public Map<Object, Object> hGetAll(final String key) {
        return template.opsForHash().entries(key);
    }


    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Duration duration) {
        //获取存储到Redis中的数据key
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        //从Redis查询缓存数据
        String str = template.opsForValue().get(key);
        //缓存存在数据，直接返回
        if (StringUtils.hasText(str)) {
            //返回数据
            return this.getResult(str, type);
        }
        //缓存中存储的是空字符串
        if (EMPTY_VALUE.equals(str)) {
            return null;
        }
        //从数据库查询数据
        R r = dbFallback.apply(id);
        //数据数据为空
        if (r == null) {
            template.opsForValue().set(key, EMPTY_VALUE, Duration.ofSeconds(CACHE_NULL_TTL));
            return null;
        }
        //缓存数据
        template.opsForValue().set(key, JsonUtils.toJson(r), duration);
        return r;
    }

    /**
     * 刷新缓存
     *
     * @param keyPrefix
     * @param id
     * @param dbFallback
     * @param duration
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R refresh(String keyPrefix, ID id, Function<ID, R> dbFallback, Duration duration) {
        //获取存储到Redis中的数据key
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        //从数据库查询数据
        R r = dbFallback.apply(id);
        //数据数据为空
        if (r == null) {
            template.opsForValue().set(key, RedisUtil.EMPTY_VALUE, Duration.ofSeconds(RedisUtil.CACHE_NULL_TTL));
            return null;
        }
        //缓存数据
        template.opsForValue().set(key, JsonUtils.toJson(r), duration);
        return r;
    }

    /**
     * 将对象类型的json字符串转换成泛型类型
     *
     * @param obj  未知类型对象
     * @param type 泛型Class类型
     * @param <R>  泛型
     * @return 泛型对象
     */
    private <R> R getResult(Object obj, Class<R> type) {
        if (obj == null) {
            return null;
        }
        //简单类型
        if (TypeConversion.isSimpleType(obj)) {
            return Convert.convert(type, obj);
        }
        return JsonUtils.toObject(JsonUtils.toJson(obj), type);
    }
}
