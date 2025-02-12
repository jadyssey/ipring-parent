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
public class ImgDownloadExcelVO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "图片相对地址")
    private String images;

    @ExcelColumn(1)
    @ApiModelProperty(value = "POD图片1")
    private String image1;

    @ExcelColumn(2)
    @ApiModelProperty(value = "POD图片2")
    private String image2;

    @ExcelColumn(3)
    @ApiModelProperty(value = "POD图片3")
    private String image3;

    @ExcelColumn(4)
    @ApiModelProperty(value = "POD图片4")
    private String image4;

    @ExcelColumn(5)
    @ApiModelProperty(value = "POD图片5")
    private String image5;
}
