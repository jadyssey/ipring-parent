package org.ipring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.ipring.model.dobj.trade.TradeOrderDO;
import org.ipring.model.entity.order.OrderDTO;
import org.ipring.model.entity.order.TradeOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author lgj
 * @date 2024/4/3
 **/
@Mapper
public interface OrderMapper extends BaseMapper<TradeOrderDO> {
    default LambdaQueryChainWrapper<TradeOrderDO> query() {
        return new LambdaQueryChainWrapper<>(this);
    }

    default LambdaUpdateChainWrapper<TradeOrderDO> update() {
        return new LambdaUpdateChainWrapper<>(this);
    }

    /**
     * 查询账号特定订单类型
     *
     * @param accountId
     * @param orderTypeList
     * @return
     */
    default List<TradeOrderDO> getOrderByAccount(Long accountId, List<Integer> orderTypeList) {
        return query().eq(TradeOrderDO::getAccountId, accountId)
                .in(CollectionUtils.isNotEmpty(orderTypeList), TradeOrderDO::getOrderType, orderTypeList)
                .list();
    }

    /**
     * 根据订单id查询详情
     *
     * @param orderDTO
     * @return
     */
    default TradeOrderEntity getByUqId(OrderDTO orderDTO) {
        return TradeOrderEntity.of(query().eq(TradeOrderDO::getAccountId, orderDTO.getAccountId())
                .eq(TradeOrderDO::getOrderId, orderDTO.getOrderId())
                .one());
    }

    /**
     * 根据主键id查询订单信息
     * @param id
     * @return
     */
    default TradeOrderEntity getById(Long id) {
        return TradeOrderEntity.of(query().eq(TradeOrderDO::getId, id).one());
    }

    int createSubCloseOrder(@Param("originId") Long originId,
                            @Param("orderId") Long orderId,
                            @Param("orderStatus") Integer orderStatus,
                            @Param("closePrice") BigDecimal closePrice,
                            @Param("realClosePrice") BigDecimal realClosePrice,
                            @Param("closeTime") Long closeTime,
                            @Param("closeType") Integer closeType,
                            @Param("profit") BigDecimal profit,
                            @Param("profitAsProfitCurrency") BigDecimal profitAsProfitCurrency,
                            @Param("profitRate") BigDecimal profitRate,
                            @Param("priceChangeRate") Long priceChangeRate,
                            @Param("commissions") BigDecimal commissions);
}
