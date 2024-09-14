package org.ipring.model.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ScanMqEntity {
    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "运单号")
    private String waybillNo;

    @ApiModelProperty(value = "扫描类型 ExpressStatusEnum")
    private Integer scanType;

    @ApiModelProperty(value = "生成方式（0 手动触发 1 系统补录）")
    private Integer generationMethod;

    @ApiModelProperty(value = "扫描方式:1扫描,2手动输入")
    private Integer scanMode;

    @ApiModelProperty(value = "扫描人ID")
    private Long createUserId;

    @ApiModelProperty(value = "扫描组织ID")
    private Long createGroupId;
}