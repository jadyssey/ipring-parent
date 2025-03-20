package org.ipring.model.delivery;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Questionnaire {
    @ApiModelProperty(value = "回答1-运输标签")
    private String shippingLabelQuestion;

    @ApiModelProperty(value = "回答2")
    private String q2;

    @ApiModelProperty(value = "回答3")
    private String q3;
}