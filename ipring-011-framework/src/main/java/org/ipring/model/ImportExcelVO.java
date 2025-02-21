package org.ipring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ipring.excel.ExcelColumn;

/**
 * @author liuguangjin
 * @date 2/18/2025
 */
@Data
public class ImportExcelVO {
    @ExcelColumn(0)
    @ApiModelProperty("时间")
    private String time;

    @ExcelColumn(1)
    @ApiModelProperty("消息")
    private String message;
}
