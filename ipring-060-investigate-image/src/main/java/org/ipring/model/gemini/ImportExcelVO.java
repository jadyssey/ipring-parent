package org.ipring.model.gemini;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ipring.common.ComConstants;
import org.ipring.excel.ExcelColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @ApiModelProperty(value = "详细地址")
    private String address;

    @ExcelColumn(2)
    @ApiModelProperty(value = "收件地外门牌")
    private Integer doorNumberExe;

    @ExcelColumn(3)
    @ApiModelProperty(value = "签收类型")
    private String signType;

    @ExcelColumn(4)
    @ApiModelProperty(value = "POD图片原始地址")
    private String images;

    @ExcelColumn(5)
    @ApiModelProperty(value = "POD图片1")
    private String image1;

    @ExcelColumn(6)
    @ApiModelProperty(value = "POD图片2")
    private String image2;

    @ExcelColumn(7)
    @ApiModelProperty(value = "POD图片3")
    private String image3;

    @ExcelColumn(8)
    @ApiModelProperty(value = "POD图片4")
    private String image4;

    @ExcelColumn(9)
    @ApiModelProperty(value = "POD图片5")
    private String image5;

    @ExcelColumn(10)
    @ApiModelProperty(value = "人工-是否合规")
    private String podFlag;

    @ExcelColumn(11)
    @ApiModelProperty(value = "人工-不合规原因")
    private String reason;

    @ExcelColumn(12)
    @ApiModelProperty(value = "用量")
    private String usageMetadata;

    @ExcelColumn(13)
    @ApiModelProperty(value = "使用模型")
    private String model;

    @ExcelColumn(14)
    @ApiModelProperty(value = "耗时（单位毫秒）")
    private Long time;

    @ExcelColumn(15)
    @ApiModelProperty(value = "AI-提示词")
    private String question;

    @ExcelColumn(16)
    @ApiModelProperty(value = "运单匹配率")
    private Double matchingRate;

    @ExcelColumn(17)
    @ApiModelProperty(value = "AI-原始回答")
    private String answer;

    @Getter
    @AllArgsConstructor
    public enum SignType {

        Mailbox("Mailbox", "", ComConstants.question04),
        Person_sign("Person sign", "", ComConstants.question02),
        Received_on_behalf_of_another_person("Received on behalf of another person", "", ComConstants.question02),
        Guard_Doormen("Guard/Doormen", "", ComConstants.question03),
        Delivered_to_recipients_designated_address("Delivered to recipients designated address", "", ComConstants.question01),

        COMMON("通用提问问题", ComConstants.systemSetup, ComConstants.en2_classifyQuestion),
        ;

        private final String text;
        private final String systemSetup;
        private final String question;

        public static final Map<String, SignType> all_map = Arrays.stream(SignType.values()).collect(Collectors.toMap(SignType::getText, Function.identity()));
    }


    // 将当前模型中的字段img1 img2 img3.. 转成imglist
    public List<String> getImgToImgList() {
        return Stream.of(this.image1, this.image2, this.image3, this.image4, this.image5).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

}
