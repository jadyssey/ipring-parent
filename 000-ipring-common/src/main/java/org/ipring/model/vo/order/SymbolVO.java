package org.ipring.model.vo.order;

import org.ipring.model.dobj.trade.TradeSymbolDO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 *
 * @author lgj
 * @date 2024/4/11
 **/
@Data
public class SymbolVO extends TradeSymbolDO {
    public static SymbolVO of(TradeSymbolDO tradeSymbolDO) {
        SymbolVO symbolVO = new SymbolVO();
        BeanUtils.copyProperties(tradeSymbolDO, symbolVO);
        return symbolVO;
    }
}
