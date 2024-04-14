package org.ipring.util;

import org.ipring.cache.CacheService;
import org.ipring.enums.account.CurrencyEnum;
import org.ipring.enums.common.BoolTypeInt;
import org.ipring.enums.order.CalculationEnum;
import org.ipring.enums.order.CommissionsTypeEnum;
import org.ipring.enums.order.OrderOpenAndCloseEnum;
import org.ipring.enums.order.OrderQueryEnum;
import org.ipring.enums.order.SwapTypeEnum;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.model.dobj.trade.TradeSymbolDO;
import org.ipring.model.entity.order.SymbolDTO;
import org.ipring.model.entity.order.SymbolMsgDTO;
import org.ipring.model.entity.order.TradeOrderEntity;
import org.ipring.model.inte.order.OrderInfoInte;
import org.ipring.model.inte.symbol.SymbolInfoInte;
import lombok.extern.slf4j.Slf4j;
import org.ipring.enums.order.OrderTypeEnum;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static org.ipring.enums.account.CurrencyEnum.CURRENCY_ENUM_MAP;

/**
 * 参考： https://www.kdocs.cn/l/ccgxzv2okPvA  第六章：交易计算规则
 *
 * @author lgj
 * @date 2024/4/8
 **/
@Slf4j
public abstract class TraderCalculationUtil {

    // 买价和卖价的问题 在这里直接处理完

    /**
     * 保证金
     *
     * @param symbol       品种信息
     * @param ticket       手数
     * @param marketPrice  基准货币报价
     * @param accountLever 账户杠杆
     * @return 保证金的值 基准货币对应保证金的值
     */
    public static BigDecimal calcMargin(TradeSymbolDO symbol, Double ticket, BigDecimal marketPrice, Integer accountLever) {
        log.info("开始计算保证金 symbol:{}, ticket:{}", symbol, ticket);
        final BigDecimal tempVal = CalcUtil.multiply(ticket, symbol.getMarginRate()); // 每一笔订单都要计算  手数 * 值 * 保证金比例 直接计算一个中间值出来
        if (Objects.nonNull(symbol.getInitialMargin())) {
            final BigDecimal ret = CalcUtil.multiply(tempVal, symbol.getInitialMargin());
            log.info("品种已经设定了初始保证金, 初始保证金:{}, ret:{}", symbol.getInitialMargin(), ret);
            return ret;
        }
        final CalculationEnum calculationEnum = CalculationEnum.CALCULATION_ENUM_MAP.get(symbol.getCalculation());
        if (null == calculationEnum) {
            log.error("未查询到该品种对应计算模式 symbol:{}", symbol);
            throw new ServiceException(SystemServiceCode.SystemApi.PARAM_ERROR);
        }
        final BigDecimal ret = CalcUtil.multiply(tempVal, calculationEnum.calcMargin(symbol, marketPrice, accountLever));
        log.info("通过enum:{} 计算得出ret:{}", calculationEnum.getDescription(), ret);
        return ret;
    }

    // 盈亏

    /**
     * 计算交易盈亏
     *
     * @param order       订单
     * @param marketPrice 实时报价
     * @return 交易盈亏
     */
    public static BigDecimal calcTradeProfit(SymbolInfoInte symbol, OrderInfoInte order, BigDecimal marketPrice) {
        final BigDecimal openPrice = order.getRealOpenPrice(); // 开仓价
        final String marginCurrency = symbol.getMarginCurrency(); // 预付款货币
        final String profitCurrency = symbol.getProfitCurrency(); // 盈利货币
        final BigDecimal openPrice2ProfitCurrency = currencyExchange(marginCurrency, openPrice, profitCurrency);
        final BigDecimal priceDiff = CalcUtil.subtract(marketPrice, openPrice2ProfitCurrency);
        log.debug("计算交易盈利 开仓价:{}, 预付款货币单位:{}, 盈亏货币单位:{}, 开仓价转盈亏货币:{}, 盈亏货币计算后价格差:{}",
                openPrice, marginCurrency, profitCurrency, openPrice2ProfitCurrency, priceDiff);
        return CalculationEnum.CALCULATION_ENUM_MAP.get(symbol.getCalculation()).calcTradeProfit(priceDiff, symbol, order);
    }

    /**
     * 注意这里计算的结果 [0]指的是盈亏货币的盈亏 [1]指的是存款货币的盈亏
     *
     * @param symbol          品种信息
     * @param order           订单信息
     * @param depositCurrency 账户的存款货币
     * @param symbolMsg       实时报价模型
     * @param commissions     手续费 因为平仓的时候可能需要计算一笔手续费 所以就放在外面了 但是需要注意的是 不管在哪里计算 这个手续费单位应转为存款货币的单位
     * @return [盈亏货币盈亏, 存款货币盈亏]
     */
    public static BigDecimal[] calcProfit(TradeSymbolDO symbol, OrderInfoInte order, String depositCurrency,
                                          SymbolMsgDTO symbolMsg, BigDecimal commissions, BigDecimal swap) {
        return calcProfit(symbol, order, depositCurrency, SymbolMsgUtil.closePrice(symbolMsg, order.getOrderType()), commissions, swap);
    }

    /**
     * 计算盈亏
     */
    public static BigDecimal[] calcProfit(SymbolInfoInte symbol, OrderInfoInte order, String depositCurrency,
                                          BigDecimal closePrice, BigDecimal commissions, BigDecimal swap) {
        final BigDecimal tradeProfit = calcTradeProfit(symbol, order, closePrice);
        // 因为外面可能需要提前计算手续费 改为传进来的
        // final BigDecimal commissions = calcCommissions(symbol, order, depositCurrency, OrderOpenAndCloseEnum.OrderTimingEnum.CLOSE, closePrice);
        final BigDecimal profitCurrencyProfit = CalcUtil.add(tradeProfit, swap,
                /*手续费是负数*/Optional.ofNullable(commissions).map(BigDecimal::negate).orElse(BigDecimal.ZERO));
        final BigDecimal depositCurrencyProfit = currencyExchange(symbol.getProfitCurrency(), profitCurrencyProfit, depositCurrency);
        log.debug("交易品种:{}, 交易订单:{}, 交易盈利:{}, 隔夜利息:{}, 手续费:{}, 盈利货币盈利:{}, 持仓货币盈利:{}",
                symbol, order, tradeProfit, order.getSwap(), commissions, profitCurrencyProfit, depositCurrencyProfit);
        return new BigDecimal[]{profitCurrencyProfit, depositCurrencyProfit};
    }

    /**
     * 实时订单的盈亏计算
     */
    public static BigDecimal[] calcProfit(SymbolInfoInte symbol, OrderInfoInte order, String depositCurrency, BigDecimal marketPrice) {
        return calcProfit(symbol, order, depositCurrency, marketPrice, order.getCommissions(), order.getSwap());
    }

    /**
     * @param margin         保证金
     * @param marginCurrency 保证金货币
     * @param profit         收益
     * @param profitCurrency 收益货币
     * @return 收益率
     */
    public static BigDecimal calcProfitRate(BigDecimal margin, String marginCurrency, BigDecimal profit, String profitCurrency) {
        final BigDecimal margin2ProfitCurrency = currencyExchange(marginCurrency, margin, profitCurrency);
        final BigDecimal ret = CalcUtil.divide(CalcUtil.subtract(profit, margin2ProfitCurrency), margin2ProfitCurrency);
        log.info("计算收益率 预付款:{}, 预付款货币:{}, 预付款转收益货币:{}, 收益:{}, 收益货币:{}, 结果:{}", margin, marginCurrency, margin2ProfitCurrency, profit, profitCurrency, ret);
        return ret;
    }

    /**
     * 计算手续费
     *
     * @param symbol 品种信息
     * @param ticket 订单手数
     * @return 手续费
     */
    public static BigDecimal calcCommissions(TradeSymbolDO symbol, Double ticket, String depositCurrency,
                                             OrderOpenAndCloseEnum.OrderTimingEnum timing, BigDecimal marketPrice) {
        if ((symbol.getCommissionsMode() & timing.getType()) == 0) return null;
        // 盈亏货币单位的手续费
        return CommissionsTypeEnum.COMMISSIONS_TYPE_ENUM_MAP.get(symbol.getCommissionsMode()).commissions(symbol, ticket, marketPrice, depositCurrency);
    }

    public static BigDecimal calcCommissionsWithoutNa(TradeSymbolDO symbol, Double ticket, String depositCurrency,
                                                      OrderOpenAndCloseEnum.OrderTimingEnum timing, BigDecimal marketPrice) {
        return Optional.ofNullable(calcCommissions(symbol, ticket, depositCurrency, timing, marketPrice)).orElse(BigDecimal.ZERO);
    }

    /**
     * 滑点误差校验
     *
     * @param digits    报价的小数点位数
     * @param orderType 订单类型
     * @param slippage  交易误差
     * @param price     请求的价格
     * @param realPrice 真实的价格
     * @return 是否在误差范围内
     */
    public static boolean slippageVerify(Integer digits, Integer orderType, Long slippage, BigDecimal price, BigDecimal realPrice) {
        if (!OrderQueryEnum.MARKET.getOrderTypeList().contains(orderType)) return true;
        // 校验订单类型为市价单
        return Optional.ofNullable(slippage)
                .filter(sp -> sp == 0)
                .map(sp -> {
                    BigDecimal expect = CalcUtil.multiply(CalcUtil.pow(10, -digits), sp);
                    BigDecimal actual = CalcUtil.subtract(realPrice, price).abs();
                    return expect.compareTo(actual) > 0;
                })
                .orElse(true);
    }
    // 保证金追缴水平
    // 强制平仓

    /**
     * 隔夜利息计算
     *
     * @param symbol
     * @return
     */
    // todo 入参模型改为接口
    public static BigDecimal swap(TradeSymbolDO symbol, TradeOrderEntity order, String depositCurrency) {
        if (BoolTypeInt.isFalse(symbol.getSwapStatus())) return BigDecimal.ZERO;
        return SwapTypeEnum.ALL_ENUM_MAP.get(symbol.getSwapType()).calcSwap(symbol, order, depositCurrency);
    }

    /**
     * 外汇结算模式的汇率转换
     * calculation = 4
     * EUR -> USD
     * 如果转换失败则抛异常
     *
     * @param fromCode     基准货币代码
     * @param fromCurrency 基准货币价值
     * @param toCode       计价货币代码
     * @return 计价货币价值
     */
    public static BigDecimal currencyExchange(String fromCode, BigDecimal fromCurrency, String toCode) {
        if (fromCode.equals(toCode)) return fromCurrency;
        return currencyExchange(fromCode, fromCurrency, toCode, true);
    }

    private static BigDecimal currencyExchange(String fromCode, BigDecimal fromCurrency, String toCode, boolean change) {
        if (Objects.isNull(fromCurrency)) throw new ServiceException(SystemServiceCode.TradeCalcula.PARAM_ERROR);
        return CacheService.symbolMsg()
                .getLocalCache(SymbolDTO.of(getMarketType(fromCode, toCode), fromCode + toCode))
                // 汇率EUR->USD，如果使用EURUSD品种开单，则是sell单；如果使用USDEUR品种开单，则是buy单
                .map(msg -> SymbolMsgUtil.openPrice(msg, change ? OrderTypeEnum.SELL.getType() : OrderTypeEnum.BUY.getType()))
                .map(price -> change ? CalcUtil.multiply(fromCurrency, price) : CalcUtil.divide(fromCurrency, price))
                .orElseGet(() -> {
                    if (change) {
                        // 位置互换再试一遍，两个品种的位置只交换一遍
                        return currencyExchange(toCode, fromCurrency, fromCode, false);
                    } else {
                        // 尝试枚举中的所有货币作为中间货币转换汇率
                        for (CurrencyEnum otherCurrencyEnum : CURRENCY_ENUM_MAP) {
                            BigDecimal otherCurrency = currencyExchange(toCode, fromCurrency, otherCurrencyEnum.getType());
                            BigDecimal res = currencyExchange(otherCurrencyEnum.getType(), otherCurrency, fromCode);
                            if (res != null) return res;
                        }
                        log.error("汇率转换失败 from={}, to={}", toCode, fromCode);
                        throw new ServiceException(SystemServiceCode.TradeCalcula.SYMBOL_PRICE);
                    }
                });
    }

    /**
     * 统一认为都是外汇市场的汇率转换：marketType 有美元就走8100, 无美元就走8200
     *
     * @param fromCode
     * @param toCode
     * @return
     */
    private static Integer getMarketType(String fromCode, String toCode) {
        if (CurrencyEnum.USD.getType().equalsIgnoreCase(fromCode)) return 8100;
        if (CurrencyEnum.USD.getType().equalsIgnoreCase(toCode)) return 8100;
        return 8200;
    }
    // 特殊规则

    public static void main(String[] args) {

    }
}
