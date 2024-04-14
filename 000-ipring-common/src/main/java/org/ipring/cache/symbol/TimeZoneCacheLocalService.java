package org.ipring.cache.symbol;

import org.ipring.model.entity.order.SymbolTimeZoneDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.ipring.constant.CommonConstants.SYMBOL_SPLIT;

/**
 * todo 品种对应时区缓存，这个类要优化成实现CacheService接口，需要等数据库表弄好时区后写
 * 品种时区获取
 *
 * @author lgj
 * @date 2024/4/7
 **/
@Component
public class TimeZoneCacheLocalService extends CacheLocalService<SymbolTimeZoneDTO> {

    private static final ConcurrentHashMap<String, SymbolTimeZoneDTO> SYMBOL_TIME_MAP = new ConcurrentHashMap<>();

    @Override
    public Map<String, SymbolTimeZoneDTO> getHolder() {
        return SYMBOL_TIME_MAP;
    }

    @Override
    public void clear() {
        SYMBOL_TIME_MAP.clear();
    }

    @Override
    public void put(String key, SymbolTimeZoneDTO o) {
        SYMBOL_TIME_MAP.put(key, o);
    }

    @Override
    public Optional<SymbolTimeZoneDTO> get(Integer marketType, String symbolId) {
        SymbolTimeZoneDTO timeZoneDTO = SYMBOL_TIME_MAP.get(getKey(marketType, null));
        if (timeZoneDTO != null) return Optional.of(timeZoneDTO);
        Optional<SymbolTimeZoneDTO> symbolTimeZoneDTO = Optional.ofNullable(SYMBOL_TIME_MAP.get(getKey(marketType, symbolId)));
        // todo 时区拿不到需要走redis或者数据库，不存在拿不到的场景
        return symbolTimeZoneDTO;
    }

    private static String getKey(Integer marketType, String symbolId) {
        return marketType + SYMBOL_SPLIT + symbolId;
    }
}
