package org.ipring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.SymbolDTO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author lgj
 * @date 2024/4/2
 **/
@Mapper
public interface SymbolMapper extends BaseMapper<TradeSymbolDO> {

    default LambdaQueryChainWrapper<TradeSymbolDO> query() {
        return new LambdaQueryChainWrapper<>(this);
    }

    default LambdaUpdateChainWrapper<TradeSymbolDO> update() {
        return new LambdaUpdateChainWrapper<>(this);
    }


    default TradeSymbolDO getSymbolByMarketAndId(SymbolDTO symbolDTO) {
        return query().eq(TradeSymbolDO::getMarketType, symbolDTO.getMarketType())
                .eq(TradeSymbolDO::getSymbolId, symbolDTO.getSymbolId())
                .eq(TradeSymbolDO::getActive, BoolTypeInt.YES.getType())
                .one();
    }
}
