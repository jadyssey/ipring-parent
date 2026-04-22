package org.ipring.jacg.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author liuguangjin
 * @date 2026/4/22
 **/
@Data
public class ContentVO {
    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "内容")
    private String content;
}
