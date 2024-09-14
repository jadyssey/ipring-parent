package org.ipring.behavioral.chain3;

/**
 * @author lgj
 * @date 2024/4/17
 **/
public interface SymbolInte {
    Integer getTradeRight();

    Integer getExpirationType();

    Integer getMarketType();

    String getSymbolId();

    String getTradeSessions();

    Double getMinVolume();

    Double getMaxVolume();

    Double getVolumeStep();

}
