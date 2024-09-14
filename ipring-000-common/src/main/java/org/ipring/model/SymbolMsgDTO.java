package org.ipring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.ipring.constant.CommonConstants.SYMBOL_SPLIT;


/**
 * 8100_EURUSD, 1.07001,1.07089,1.07100,1.06995,1.07089       ,1.07136,1.06989,0   ,1713926238,+0.00088,+0.0008,3  ,5   ,0   ,0    ,1
 * 品种id      ,昨收价   ,卖价    ,买价    ,开盘价  ,收盘价(最新报价),最高价  ,最低价   ,点差,时间       ,涨跌额   ,涨跌幅  ,时区,精度,成交量,成交额,tick(0-虚拟，1-盘中, 2-盘前, 3-盘后)可能没有
 * -                                                                               ,9         ,10      ,11    ,12  ,13 ,14   ,15   ,16
 *
 * @author lgj
 * @date 2024/4/3
 **/
@Data
public class SymbolMsgDTO {
    /**
     * 品种信息，包含两个字段
     */
    private SymbolDTO symbol;

    @ApiModelProperty("昨收盘价")
    private BigDecimal prePrice;
    @ApiModelProperty("卖价")
    private BigDecimal askPrice;
    @ApiModelProperty("买价")
    private BigDecimal bidPrice;

    @ApiModelProperty("开盘价")
    private BigDecimal openPrice;
    @ApiModelProperty("收盘价（最新价）")
    private BigDecimal closePrice;
    @ApiModelProperty("最高价")
    private BigDecimal high;
    @ApiModelProperty("最低价")
    private BigDecimal low;

    @ApiModelProperty("点差")
    private Integer spread;
    @ApiModelProperty("时间戳")
    private Long time;
    @ApiModelProperty("涨跌额")
    private BigDecimal change;
    @ApiModelProperty("日涨跌幅")
    private BigDecimal changeRate;
    @ApiModelProperty("时区")
    private Integer timezone;

    @ApiModelProperty("报价保留小数的长度")
    private Integer precision;
    @ApiModelProperty("成交量")
    private BigDecimal vol;
    @ApiModelProperty("成交额")
    private BigDecimal turnOver;

    @ApiModelProperty("0-虚拟，1-盘中，5-盘前，6-盘后")
    private Integer tickType;

    @ApiModelProperty("当前服务收到报价的时间")
    private Long updateTime;

    /*@ApiModelProperty("数据中心返回数据需要自己处理的长度")
    private Integer digits;
    @ApiModelProperty("是否停牌 是-true")
    private Boolean isSuspension;
    @ApiModelProperty("0-正常 1-停牌 2-退市")
    private Integer stockStatus;
    @ApiModelProperty("市场状态 0-未开盘 1-交易中 2-午休 3-已收盘 4-初始化 5-休市")
    private Integer marketStatus;
    @ApiModelProperty("其他市场状态 1-盘前 2-盘后")
    private Integer marketStatusMore;
    @ApiModelProperty("最近52周内最高价")
    private BigDecimal price52WeekHigh;
    @ApiModelProperty("最近52周内最高价时间戳")
    private Long time52WeekHigh;
    @ApiModelProperty("最近52周内最低价")
    private BigDecimal price52WeekLow;
    @ApiModelProperty("最近52周内最低价时间戳")
    private Long time52WeekLow;
    @ApiModelProperty("盘前价格")
    private BigDecimal preMarketPrice;
    @ApiModelProperty("盘前价格时间")
    private Long preMarketTime;
    @ApiModelProperty("盘前涨跌额")
    private BigDecimal preMarketChange;
    @ApiModelProperty("盘前涨跌幅")
    private BigDecimal preMarketChangeRate;
    @ApiModelProperty("盘前最高价")
    private BigDecimal preMarketHigh;
    @ApiModelProperty("盘前最低价")
    private BigDecimal preMarketLow;
    @ApiModelProperty("盘前总成交量")
    private BigDecimal preMarketVol;
    @ApiModelProperty("盘前总成交额")
    private BigDecimal preMarketTurnOver;
    @ApiModelProperty("盘后价格")
    private BigDecimal afterMarketPrice;
    @ApiModelProperty("盘后价格时间")
    private Long afterMarketTime;
    @ApiModelProperty("盘后涨跌额")
    private BigDecimal afterMarketChange;
    @ApiModelProperty("盘后涨跌幅")
    private BigDecimal afterMarketChangeRate;
    @ApiModelProperty("盘后最高价")
    private BigDecimal afterMarketHigh;
    @ApiModelProperty("盘后最低价")
    private BigDecimal afterMarketLow;
    @ApiModelProperty("盘后总成交量")
    private BigDecimal afterMarketVol;
    @ApiModelProperty("盘后总成交额")
    private BigDecimal afterMarketTurnOver;*/
    /**
     * 上一次查询账号表时返回null的时间
     */
    private Long nullDataTime;

    public static SymbolMsgDTO nullEntity(String symbol) {
        SymbolMsgDTO cacheEntity = new SymbolMsgDTO();
        cacheEntity.setSymbol(SymbolDTO.of(symbol));
        cacheEntity.setNullDataTime(System.currentTimeMillis());
        return cacheEntity;
    }

    public static SymbolMsgDTO of(String[] msgArr) {
        SymbolMsgDTO symbolMsg = new SymbolMsgDTO();
        String[] symbol = msgArr[0].split(SYMBOL_SPLIT);
        if (symbol.length < 2) return null;
        try {
            SymbolDTO of = SymbolDTO.of(null, symbol[1]);
            Optional.ofNullable(symbol[0]).filter(StringUtils::isNumeric)
                    .map(Integer::parseInt).ifPresent(of::setMarketType);
            symbolMsg.setSymbol(of);
        } catch (Exception e) {
            System.out.println("msgArr = " + Arrays.toString(msgArr));
            e.printStackTrace();
            return null;
        }
        symbolMsg.setPrePrice(new BigDecimal(msgArr[1]));
        symbolMsg.setAskPrice(new BigDecimal(msgArr[2]));
        symbolMsg.setBidPrice(new BigDecimal(msgArr[3]));
        symbolMsg.setOpenPrice(new BigDecimal(msgArr[4]));
        symbolMsg.setClosePrice(new BigDecimal(msgArr[5]));
        symbolMsg.setHigh(new BigDecimal(msgArr[6]));
        symbolMsg.setLow(new BigDecimal(msgArr[7]));
        symbolMsg.setSpread(Integer.parseInt(msgArr[8]));
        symbolMsg.setTime(Long.parseLong(msgArr[9]));
        symbolMsg.setChange(new BigDecimal(msgArr[10]));
        symbolMsg.setChangeRate(new BigDecimal(msgArr[11]));
        symbolMsg.setTimezone(Integer.parseInt(msgArr[12]));
        symbolMsg.setPrecision(Integer.parseInt(msgArr[13]));
        symbolMsg.setVol(new BigDecimal(msgArr[14]));
        symbolMsg.setTurnOver(new BigDecimal(msgArr[15]));
        if (msgArr.length > 16) symbolMsg.setTickType(Integer.parseInt(msgArr[16]));

        symbolMsg.setUpdateTime(System.currentTimeMillis());
        return symbolMsg;
    }
}
