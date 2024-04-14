package org.ipring.model.entity.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import lombok.Data;

import static org.ipring.constant.CommonConstants.SYMBOL_LENTH;
import static org.ipring.constant.CommonConstants.SYMBOL_SPLIT;

/**
 * @author lgj
 * @date 2024/4/3
 **/

@Data
public class SymbolDTO {
    private Integer marketType;
    private String symbolId;

    public static SymbolDTO of(String symbol) {
        String[] sb = symbol.split(SYMBOL_SPLIT);
        if (sb.length < SYMBOL_LENTH) throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
        return of(Integer.parseInt(sb[0]), sb[1]);
    }

    public static SymbolDTO of(Integer marketType, String symbolId) {
        SymbolDTO symbolDTO = new SymbolDTO();
        symbolDTO.setMarketType(marketType);
        symbolDTO.setSymbolId(symbolId);
        return symbolDTO;
    }

    @JsonIgnore
    public String getUniqKey() {
        return this.getMarketType() + SYMBOL_SPLIT + this.getSymbolId();
    }
}
