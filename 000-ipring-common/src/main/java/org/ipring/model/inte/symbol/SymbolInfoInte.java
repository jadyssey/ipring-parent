package org.ipring.model.inte.symbol;

/**
 * @author: Rainful
 * @date: 2024/04/11 20:52
 * @description:
 */
public interface SymbolInfoInte {

    String getProfitCurrency();

    String getMarginCurrency();

    Integer getCalculation();

    Long getContractSize();

    Long getTickPrice();

    Integer getTickSize();
}
