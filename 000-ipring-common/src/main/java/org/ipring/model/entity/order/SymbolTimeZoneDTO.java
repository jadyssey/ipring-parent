package org.ipring.model.entity.order;

import lombok.Data;

/**
 * 8400_USDX,92.968,93.017,93.052,92.985,93.017,93.062,92.946,35,1629791627,+0.049,+0.0005,+8,3,0,0
 * 品种id,昨收价,卖价,买价,开盘价,收盘价(最新报价),最高价,最低价,点差,时间,涨跌额,涨跌幅,时区,精度,成交量,成交额
 *
 * @author lgj
 * @date 2024/4/3
 **/
@Data
public class SymbolTimeZoneDTO {
    /**
     * 品种信息，包含两个字段
     */
    private SymbolDTO symbol;

    /**
     * 时区
     */
    private String timeZone;

    public static SymbolTimeZoneDTO of(String symbolId, Integer marketType, String timeZone) {
        SymbolTimeZoneDTO symbolMsg = new SymbolTimeZoneDTO();
        symbolMsg.setSymbol(SymbolDTO.of(marketType, symbolId));
        symbolMsg.setTimeZone(timeZone);
        return symbolMsg;
    }
}
