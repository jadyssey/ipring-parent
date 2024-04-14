package org.ipring.model.entity.order;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

import static org.ipring.constant.CommonConstants.SYMBOL_SPLIT;

/**
 * 8400_USDX,92.968,93.017,93.052,92.985,93.017,93.062,92.946,35,1629791627,+0.049,+0.0005,+8,3,0,0
 * 品种id,昨收价,卖价,买价,开盘价,收盘价(最新报价),最高价,最低价,点差,时间,涨跌额,涨跌幅,时区,精度,成交量,成交额
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
    @ApiModelProperty("涨跌幅")
    private BigDecimal changeRate;

    @ApiModelProperty("报价保留小数的长度")
    private Integer precision;
    @ApiModelProperty("成交量")
    private BigDecimal vol;
    @ApiModelProperty("成交额")
    private BigDecimal turnOver;

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

    public static SymbolMsgDTO of(String[] msgArr) {
        SymbolMsgDTO symbolMsg = new SymbolMsgDTO();
        String[] symbol = msgArr[0].split(SYMBOL_SPLIT);
        symbolMsg.setSymbol(SymbolDTO.of(Integer.parseInt(symbol[0]), symbol[1]));
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
        symbolMsg.setPrecision(Integer.parseInt(msgArr[12]));
        symbolMsg.setVol(new BigDecimal(msgArr[13]));
        symbolMsg.setTurnOver(new BigDecimal(msgArr[14]));
        symbolMsg.setUpdateTime(System.currentTimeMillis());
        return symbolMsg;
    }
}
