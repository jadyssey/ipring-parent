package org.ipring.cache;


import org.ipring.model.entity.order.SymbolDTO;
import org.ipring.model.entity.order.SymbolMsgDTO;

import java.util.Optional;

/**
 * @author lgj
 * @date 2024/4/7
 **/
public interface CacheService<R, ID> {
    /**
     * 清理本地缓存
     *
     * @param id
     */
    void clearLocalCache(ID id);

    /**
     * 清理多级缓存
     *
     * @param keyPrefix
     * @param id
     * @return
     */
    boolean clearMultCache(String keyPrefix, ID id);

    /**
     * 根据多级缓存获取数据
     *
     * @param keyPrefix
     * @param id
     * @return
     */
    Optional<R> getMultCache(String keyPrefix, ID id);

    /**
     * 获取本地缓存
     *
     * @param id
     * @return
     */
    Optional<R> getLocalCache(ID id);

    /**
     * 根据缓存值刷新缓存
     *
     * @param keyPrefix
     * @param id
     * @param val
     */
    void refresh(String keyPrefix, ID id, R val);


    /**
     * 刷新缓存
     *
     * @param keyPrefix
     * @param id
     * @return
     */
    R refresh(String keyPrefix, ID id);

    // todo 取缓存的全部数据
    // todo refresh多条缓存的
    // todo refresh local cache


    /**
     * 品种报价缓存
     * <p>
     * 用到的地方太多了，注入的方式很麻烦，就先这样用着
     *
     * @return
     */
    static CacheService<SymbolMsgDTO, SymbolDTO> symbolMsg() {
        return SymbolMsgCacheService.INSTANCE;
    }
}
