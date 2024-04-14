package org.ipring.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

import static org.ipring.constant.CommonConstants.SYMBOL_SPLIT;

/**
 * @author: Rainful
 * @date: 2024/04/11 15:41
 * @description:
 */
@Data
public class SymbolUniqEntity {

    @ApiModelProperty(value = "品种", example = "8100_EURUSD")
    @NotEmpty(message = "{common.cant_null}")
    private String symbol;

    @JsonIgnore
    public Integer getMarketType() {
        return Integer.parseInt(symbol.split(SYMBOL_SPLIT)[0]);
    }

    @JsonIgnore
    public String getSymbolId() {
        return symbol.split(SYMBOL_SPLIT)[1];
    }
}
