package org.ipring.model.httpclient.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: Rainful
 * @date: 2024/04/11 15:41
 * @description:
 */
@Data
public class SymbolUniqEntity {

    @ApiModelProperty(value = "品种", example = "8100_EURUSD")
    private String symbol;
}
