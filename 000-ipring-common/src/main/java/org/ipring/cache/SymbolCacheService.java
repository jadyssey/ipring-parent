package org.ipring.cache;

import org.ipring.constant.ExpireConstant;
import org.ipring.mapper.SymbolMapper;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.SymbolDTO;
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
@Component(value = "symbolCacheService")
@RequiredArgsConstructor
public class SymbolCacheService implements CacheService<TradeSymbolDO, SymbolDTO> {

    private final RedisUtil redisUtil;
    private final SymbolMapper symbolMapper;

    /**
     * todo guava缓存
     */
    static final ConcurrentHashMap<String, TradeSymbolDO> SYMBOL_MAP = new ConcurrentHashMap<>(40960);

    @Override
    public void clearLocalCache(SymbolDTO id) {
        SYMBOL_MAP.remove(id.getUniqKey());
    }

    @Override
    public boolean clearMultCache(String keyPrefix, SymbolDTO id) {
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        boolean del = redisUtil.del(key);
        clearLocalCache(id);
        return del;
    }

    @Override
    public Optional<TradeSymbolDO> getLocalCache(SymbolDTO id) {
        return Optional.ofNullable(SYMBOL_MAP.get(id.getUniqKey()));
    }

    @Override
    public Optional<TradeSymbolDO> getMultCache(String keyPrefix, SymbolDTO id) {
        TradeSymbolDO tradeSymbolDO = Optional.ofNullable(SYMBOL_MAP.get(id.getUniqKey()))
                .orElseGet(() -> {
                    TradeSymbolDO temp = redisUtil.queryWithPassThrough(keyPrefix, id,
                            TradeSymbolDO.class, symbolMapper::getSymbolByMarketAndId, ExpireConstant.SYMBOL_EXPIRE);
                    if (temp == null) return null;
                    // todo 定时过期本地缓存
                    SYMBOL_MAP.put(id.getUniqKey(), temp);
                    return temp;
                });
        return Optional.ofNullable(tradeSymbolDO);
    }

    @Override
    public void refresh(String keyPrefix, SymbolDTO id, TradeSymbolDO val) {
        SYMBOL_MAP.put(id.getUniqKey(), val);
        String key = RedisKeyUtil.getKey(keyPrefix, id);
        redisUtil.set(key, val, ExpireConstant.SYMBOL_EXPIRE);
    }

    @Override
    public TradeSymbolDO refresh(String keyPrefix, SymbolDTO id) {
        TradeSymbolDO resp = redisUtil.refresh(keyPrefix, id, symbolMapper::getSymbolByMarketAndId, ExpireConstant.SYMBOL_EXPIRE);
        SYMBOL_MAP.put(id.getUniqKey(), resp);
        return resp;
    }
}
