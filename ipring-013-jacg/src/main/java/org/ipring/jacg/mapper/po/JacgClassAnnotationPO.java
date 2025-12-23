package org.ipring.jacg.mapper.po;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 类上的注解信息表实体类
 *
 * @author 自动生成
 * @date 2025-12-23
 */
@Data
@ApiModel(value = "JacgClassAnnotationJacg", description = "类上的注解信息表")
public class JacgClassAnnotationPO {

    /**
     * 记录id，从1开始
     */
    @ApiModelProperty(value = "记录id，从1开始", required = true, example = "1")
    private Integer recordId;

    /**
     * 唯一类名
     */
    @ApiModelProperty(value = "唯一类名", required = true, example = "UserController")
    private String simpleClassName;

    /**
     * 注解类名
     */
    @ApiModelProperty(value = "注解类名", required = true, example = "org.springframework.web.bind.annotation.RestController")
    private String annotationName;

    /**
     * 注解属性名称，空字符串代表无注解属性
     */
    @ApiModelProperty(value = "注解属性名称，空字符串代表无注解属性", required = true, example = "value")
    private String attributeName;

    /**
     * 注解属性类型，参考AnnotationAttributesTypeEnum类
     */
    @ApiModelProperty(value = "注解属性类型，参考AnnotationAttributesTypeEnum类", example = "[\"/apple/hubAssignTask\"]")
    private String attributeType;

    /**
     * 注解属性值
     */
    @ApiModelProperty(value = "注解属性值", example = "/user")
    private String attributeValue;

    /**
     * 完整类名
     */
    @ApiModelProperty(value = "完整类名", required = true, example = "com.example.controller.UserController")
    private String className;
}