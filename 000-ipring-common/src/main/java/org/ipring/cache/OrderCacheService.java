package org.ipring.cache;

import org.ipring.constant.ExpireConstant;
import org.ipring.mapper.OrderMapper;
import org.ipring.model.entity.order.OrderDTO;
import org.ipring.model.entity.order.TradeOrderEntity;
import org.ipring.util.RedisKeyUtil;
import org.ipring.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 品种实时报价缓存
 *
 * @author lgj
 * @date 2024/4/7
 **/
@Slf4j
@Component(value = "orderCacheService")
@RequiredArgsConstructor
public class OrderCacheService implements CacheService<TradeOrderEntity, OrderDTO> {

    private final RedisUtil redisUtil;
    private final OrderMapper orderMapper;

    /**
     * todo guava缓存
     */
    static final ConcurrentHashMap<String, TradeOrderEntity> ORDER_MAP = new ConcurrentHashMap<>(20480);

    @Override
    public void clearLocalCache(OrderDTO id) {
        ORDER_MAP.remove(id.getUniqKey());
    }

    @Override
    public boolean clearMultCache(String keyPrefix, OrderDTO id) {
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        boolean del = redisUtil.del(key);
        clearLocalCache(id);
        return del;
    }

    @Override
    public Optional<TradeOrderEntity> getLocalCache(OrderDTO id) {
        return Optional.ofNullable(ORDER_MAP.get(id.getUniqKey()));
    }

    @Override
    public Optional<TradeOrderEntity> getMultCache(String keyPrefix, OrderDTO id) {
        TradeOrderEntity cache = Optional.ofNullable(ORDER_MAP.get(id.getUniqKey()))
                .orElseGet(() -> {
                    TradeOrderEntity temp = redisUtil.queryWithPassThrough(keyPrefix, id,
                            TradeOrderEntity.class, orderMapper::getByUqId, ExpireConstant.ORDER_EXPIRE);
                    if (temp == null) return null;
                    // todo 定时过期本地缓存
                    ORDER_MAP.put(id.getUniqKey(), temp);
                    return temp;
                });
        return Optional.ofNullable(cache);
    }

    @Override
    public void refresh(String keyPrefix, OrderDTO id, TradeOrderEntity val) {
        ORDER_MAP.put(id.getUniqKey(), val);
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        redisUtil.set(key, val, ExpireConstant.SYMBOL_EXPIRE);
    }

    @Override
    public TradeOrderEntity refresh(String keyPrefix, OrderDTO id) {
        TradeOrderEntity cache = redisUtil.refresh(keyPrefix, id, orderMapper::getByUqId, ExpireConstant.ORDER_EXPIRE);
        ORDER_MAP.put(id.getUniqKey(), cache);
        return cache;
    }
}
