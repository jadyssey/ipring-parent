package org.ipring.cache.tick;

import lombok.extern.slf4j.Slf4j;
import org.ipring.enums.order.OrderStatusEnum;
import org.ipring.enums.subcode.OrderServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.OrderTickDTO;
import org.ipring.model.entity.order.SymbolDTO;
import org.ipring.model.entity.order.SymbolMsgDTO;
import org.ipring.model.entity.ws.WebSocketCmd;
import org.ipring.util.TraderCalculationUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * 根据账号推送变动订单盈亏给到用户
 * 自动止盈、自动止损、挂单下单、挂单过期
 *
 * @author lgj
 * @date 2024/4/8
 **/
@Slf4j
public abstract class AbstractOrderTickTemplate {

    // todo 精简参数
    // 定时从数据库拿所有订单刷新本地缓存
    protected final ConcurrentHashMap<String, ConcurrentSkipListSet<OrderTickDTO>> SYMBOL_MAP = new ConcurrentHashMap<>(1024);
    protected final ConcurrentHashMap<String, OrderTickDTO> ORDER_MAP = new ConcurrentHashMap<>(2048);

    protected abstract void send2Account(Long accountId, WebSocketCmd order);

    protected abstract void handler(Map<Long, List<OrderTickDTO>> accMap, SymbolMsgDTO symbolMsg, Long time);

    protected abstract Optional<TradeSymbolDO> getSymbol(SymbolDTO id);


    public void refresh(OrderTickDTO order) {
        if (order == null) return;
        if (OrderStatusEnum.ALL_ENUM_MAP.get(order.getOrderStatus()).isRealtime()) {
            OrderTickDTO orderTick = ORDER_MAP.get(order.getOrderUniq());
            if (orderTick != null) {
                BeanUtils.copyProperties(order, orderTick);
                return;
            }
            ConcurrentSkipListSet<OrderTickDTO> symbolNodes = SYMBOL_MAP.computeIfAbsent(order.getSymbolUniq(), k -> new ConcurrentSkipListSet<>());
            symbolNodes.add(order);
            ORDER_MAP.put(order.getOrderUniq(), order);
        } else {
            // 非实时订单可以直接删除了
            ORDER_MAP.remove(order.getOrderUniq());
            SYMBOL_MAP.get(order.getSymbolUniq()).remove(order);
        }
    }


    public void profit(SymbolMsgDTO symbolMsg) {
        long now = System.currentTimeMillis();
        String symbolUniq = symbolMsg.getSymbol().getUniqKey();
        // 不知道这里性能够不够 要不要在symbolUniq这一行加锁 加锁会不会影响效率，我希望的锁是谁最新申请就给谁，之前给过的程序直接失败就行了，我只要最新的
        Set<OrderTickDTO> orderList = SYMBOL_MAP.get(symbolUniq);
        if (CollectionUtils.isEmpty(orderList)) return;
        Map<Long, List<OrderTickDTO>> accMap = orderList.stream().collect(Collectors.groupingBy(OrderTickDTO::getAccountId));
        handler(accMap, symbolMsg, now);
        long end = System.currentTimeMillis();
        log.debug("收到品种报价={}，同步订单收益成功 耗时={}", symbolUniq, end - now);
    }

    protected void calculateProfitAndLoss(OrderTickDTO order, BigDecimal realTimePrice) {
        Optional<TradeSymbolDO> multCache = getSymbol(SymbolDTO.of(order.getMarketType(), order.getSymbolId()));
        if (!multCache.isPresent()) {
            log.info("未查到该订单品种信息 accountId = {},  orderId = {}", order.getAccountId(), order.getOrderId());
            throw new ServiceException(OrderServiceCode.QueryOrder.PARAM_ERROR);
        }
        TradeSymbolDO symbol = multCache.get();

        // todo
        BigDecimal[] usds = TraderCalculationUtil.calcProfit(symbol, order, "USD", realTimePrice);
        order.setProfit(usds[1]);
        order.setProfitAsProfitCurrency(usds[0]);
    }

    /**
     * 报价  -> 影响的账号的盈亏 -> 推送账号盈亏
     * todo
     */
    public void accProfit(SymbolMsgDTO symbolMsg) {



    }
}
