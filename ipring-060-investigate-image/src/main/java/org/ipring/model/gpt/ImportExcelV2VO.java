package org.ipring.model.gpt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ipring.common.ComConstants;
import org.ipring.enums.subcode.SystemServiceCode;
import org.ipring.excel.ExcelColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author liuguangjin
 * @date 2025/1/9
 */
@Data
@ApiModel(value = "识别结果导出模型")
public class ImportExcelV2VO {

    @ExcelColumn(0)
    @ApiModelProperty(value = "运单号")
    private String waybillNo;

    @ExcelColumn(1)
    @ApiModelProperty(value = "详细地址")
    private String address;

    @ExcelColumn(2)
    @ApiModelProperty(value = "收件地外门牌")
    private String doorNumberExe;

    @ExcelColumn(3)
    @ApiModelProperty(value = "签收类型")
    private String signType;

    @ExcelColumn(4)
    @ApiModelProperty(value = "妥投拍照图片地址")
    private String photographImg;

    @ExcelColumn(5)
    @ApiModelProperty(value = "妥投位置图片地址")
    private String readyDeliverImg;

    @ExcelColumn(6)
    @ApiModelProperty(value = "人工-是否合规")
    private String podFlag;

    @ExcelColumn(7)
    @ApiModelProperty(value = "人工-不合规原因")
    private String reason;

    @ExcelColumn(8)
    @ApiModelProperty(value = "用量")
    private String usageMetadata;

    @ExcelColumn(9)
    @ApiModelProperty(value = "使用模型")
    private String model;

    @ExcelColumn(10)
    @ApiModelProperty(value = "耗时（单位毫秒）")
    private Long time;

    @ExcelColumn(11)
    @ApiModelProperty(value = "AI-提示词")
    private String question;

    @ExcelColumn(12)
    @ApiModelProperty(value = "AI-原始回答")
    private String answer;

    @ExcelColumn(13)
    @ApiModelProperty(value = "错误信息")
    private String errorInfo;

    @ExcelColumn(14)
    @ApiModelProperty(value = "回答1-运输标签")
    private String shippingLabelQuestion;

    @ExcelColumn(15)
    @ApiModelProperty(value = "回答2-门牌号")
    private String doorNum;

    @ExcelColumn(17)
    @ApiModelProperty(value = "回答3")
    private String q3;

    @ExcelColumn(18)
    @ApiModelProperty(value = "二维码识别结果是否匹配")
    private String qrCodeFlag;

    @ExcelColumn(19)
    @ApiModelProperty(value = "含有二维码的图片链接为")
    private String qrCodeUrl;


    @Getter
    @AllArgsConstructor
    public enum SignType {

        Mailbox("Mailbox", "", ComConstants.question04, ""),
        Person_sign("Person sign", "", ComConstants.question02, ""),
        Received_on_behalf_of_another_person("Received on behalf of another person", "", ComConstants.question02, ""),
        Guard_Doormen("Guard/Doormen", "", ComConstants.question03, ""),
        Delivered_to_recipients_designated_address("Delivered to recipients designated address", "", ComConstants.question01, ""),


        img_classification_shippingLabelQuestion("图片分类提问-提问面单", "", ComConstants.question_img_classification_01, ComConstants.question_img_classification_01_jsonResponseFormat),
        img_classification_doorNum("图片分类提问-提问门牌号", "", ComConstants.question_img_classification_01, ComConstants.question_img_classification_01_jsonResponseFormat),

        ;
        private final String text;
        private final String systemSetup;
        private final String question;
        private final String jsonResponseFormat;

        public static final Map<String, SignType> all_map = Arrays.stream(SignType.values()).collect(Collectors.toMap(SignType::getText, Function.identity()));
    }

    // 将当前模型中的字段img1,img2,img3 转成imgList
    public List<String> getPhotographImg() {
        return Optional.ofNullable(this.photographImg).map(pi -> Arrays.stream(pi.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList())).orElseThrow(SystemServiceCode.SystemApi.PARAM_ERROR::exception);
    }

    // 将当前模型中的字段img1,img2,img3 转成imgList
    public List<String> getReadyDeliverImg() {
        return Optional.ofNullable(this.readyDeliverImg).map(rdi -> Arrays.stream(rdi.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList())).orElseThrow(SystemServiceCode.SystemApi.PARAM_ERROR::exception);
    }

}
