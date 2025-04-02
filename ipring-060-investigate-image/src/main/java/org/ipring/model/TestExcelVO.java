package org.ipring.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ipring.excel.ExcelColumn;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Data
@ApiModel(value = "识别结果导出模型")
public class TestExcelVO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "原始数据")
    private String source;

    @ExcelColumn(1)
    @ApiModelProperty(value = "时间")
    private String time;

    @ExcelColumn(2)
    @ApiModelProperty(value = "邮箱")
    private String email;
}
