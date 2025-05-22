package org.ipring.controller.model;

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
    @ApiModelProperty(value = "已清洗的地址")
    private String source;

    @ExcelColumn(1)
    @ApiModelProperty(value = "超贝提供的分组")
    private String a;

    @ExcelColumn(2)
    @ApiModelProperty(value = "75%比例文本相似度算法")
    private String b;

    @ExcelColumn(3)
    @ApiModelProperty(value = "60%比例文本相似度算法")
    private String c;
}
