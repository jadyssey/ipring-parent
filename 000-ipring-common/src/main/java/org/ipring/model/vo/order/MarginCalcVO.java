package org.ipring.model.vo.order;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/11 15:26
 * @description:
 */
@Data
public class MarginCalcVO {

    @ApiModelProperty(value = "品种", example = "8100_EURUSD")
    private String symbol;

    @ApiModelProperty(value = "保证金", example = "1.00")
    private BigDecimal margin;

    public static MarginCalcVO of(String symbol, BigDecimal margin) {
        final MarginCalcVO vo = new MarginCalcVO();
        vo.setSymbol(symbol);
        vo.setMargin(margin);
        return vo;
    }
}
