package org.ipring.model.entity.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author yb
 * @date: 2024/4/11
 */
@Getter
@Setter
public class TradeSessionsDto {
    /**
     * 交易时段
     */
    private List<List<TimeStartEnd>> trade;
    /**
     * 行情时段
     */
    private List<List<TimeStartEnd>> quetes;
    /**
     * 盘前交易
     */
    private List<List<TimeStartEnd>> preMarket;
    /**
     * 盘后交易
     */
    private List<List<TimeStartEnd>> afterHours;
    /**
     * 开盘集合竞价
     */
    private List<List<TimeStartEnd>> openCompetition;
    /**
     * 收盘集合竞价
     */
    private List<List<TimeStartEnd>> closeCompetition;
}
