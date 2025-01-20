package org.ipring.model.gemini;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.ipring.common.ComConstants;
import org.ipring.excel.ExcelColumn;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Data
@ApiModel(value = "识别结果导出模型")
public class ImportExcelVO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "运单号")
    private String waybillNo;

    @ExcelColumn(1)
    @ApiModelProperty(value = "签收类型")
    private String signType;

    @ExcelColumn(2)
    @ApiModelProperty(value = "地址1")
    private String address1;

    @ExcelColumn(3)
    @ApiModelProperty(value = "地址2")
    private String address2;

    @ExcelColumn(4)
    @ApiModelProperty(value = "地址3")
    private String address3;

    @ExcelColumn(5)
    @ApiModelProperty(value = "不合规原因")
    private String reason;

    @ExcelColumn(6)
    @ApiModelProperty(value = "POD图片1")
    private String image1;

    @ExcelColumn(7)
    @ApiModelProperty(value = "POD图片2")
    private String image2;

    @ExcelColumn(8)
    @ApiModelProperty(value = "POD图片3")
    private String image3;

    @ExcelColumn(9)
    @ApiModelProperty(value = "用量")
    private String usageMetadata;

    @ExcelColumn(10)
    @ApiModelProperty(value = "模型")
    private String model;

    @ExcelColumn(11)
    @ApiModelProperty(value = "问题")
    private String question;

    @ExcelColumn(12)
    @ApiModelProperty(value = "回答")
    private String answer;

    @Getter
    @AllArgsConstructor
    public enum SignType {

        Mailbox("Mailbox", ComConstants.question04),
        Person_sign("Person sign", ComConstants.question02),
        Received_on_behalf_of_another_person("Received on behalf of another person", ComConstants.question02),
        Guard_Doormen("Guard/Doormen", ComConstants.question03),
        Delivered_to_recipients_designated_address("Delivered to recipients designated address", ComConstants.question01),

        COMMON("通用提问问题", ComConstants.en2_classifyQuestion),
        ;

        private final String text;
        private final String question;

        public static final Map<String, SignType> all_map = Arrays.stream(SignType.values()).collect(Collectors.toMap(SignType::getText, Function.identity()));
    }
}
