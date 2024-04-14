package org.ipring.cache;

import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.model.entity.order.SymbolDTO;
import org.ipring.model.entity.order.SymbolMsgDTO;
import org.ipring.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
@Component(value = "symbolMsgCacheService")
public class SymbolMsgCacheService implements CacheService<SymbolMsgDTO, SymbolDTO>, ApplicationContextAware {

    private RedisUtil redisUtil;

    static final ConcurrentHashMap<String, SymbolMsgDTO> SYMBOL_MSG_MAP = new ConcurrentHashMap<>(40960);

    @Override
    public void clearLocalCache(SymbolDTO id) {
        SYMBOL_MSG_MAP.remove(id.getUniqKey());
    }

    @Override
    public boolean clearMultCache(String keyPrefix, SymbolDTO id) {
        redisUtil.hDel(keyPrefix, id.getUniqKey());
        clearLocalCache(id);
        return true;
    }

    @Override
    public Optional<SymbolMsgDTO> getLocalCache(SymbolDTO id) {
        return Optional.ofNullable(SYMBOL_MSG_MAP.get(id.getUniqKey()));
    }

    @Override
    public Optional<SymbolMsgDTO> getMultCache(String keyPrefix, SymbolDTO id) {
        SymbolMsgDTO tradeSymbolDO = Optional.ofNullable(SYMBOL_MSG_MAP.get(id.getUniqKey()))
                .orElseGet(() -> {
                    SymbolMsgDTO temp = (SymbolMsgDTO) redisUtil.hGet(keyPrefix, id.getUniqKey());
                    if (temp == null) return null;
                    SYMBOL_MSG_MAP.put(id.getUniqKey(), temp);
                    return temp;
                });
        return Optional.ofNullable(tradeSymbolDO);
    }

    @Override
    public void refresh(String keyPrefix, SymbolDTO id, SymbolMsgDTO val) {
        SYMBOL_MSG_MAP.put(id.getUniqKey(), val);
        // 此处不刷redis，通过定时器去刷
    }

    @Override
    public SymbolMsgDTO refresh(String keyPrefix, SymbolDTO id) {
        // 无法做此类刷新
        throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
    }

    static final CacheService<SymbolMsgDTO, SymbolDTO> INSTANCE = new SymbolMsgCacheService();

    SymbolMsgCacheService() {}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        redisUtil = applicationContext.getBean(RedisUtil.class);
    }
}
