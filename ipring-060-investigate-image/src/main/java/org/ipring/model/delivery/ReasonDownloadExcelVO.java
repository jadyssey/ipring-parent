package org.ipring.model.delivery;

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
public class ReasonDownloadExcelVO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "原始不通过原因")
    private String reason;

    @ExcelColumn(1)
    @ApiModelProperty(value = "转换后的不通过原因")
    private String reasonConvert;
}
