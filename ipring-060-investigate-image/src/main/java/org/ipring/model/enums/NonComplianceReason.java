package org.ipring.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NonComplianceReason {
    NAME_MISMATCH_OR_UNRECOGNIZED(1, " 信箱姓名不符 / 没有姓名 / 无法识别 "),
    DUPLICATE_NAME_MAILBOX(2, " 重名信箱 (客户已提供正确信箱照片)"),
    UNRECOGNIZED_RECIPIENT(3, " 无法识别收件人 (只出现手)"),
    UNKNOWN_DELIVERY_LOCATION(4, " 投递地点未知 (没有收件人 / 非家门口)"),
    INDECRYPTABLE_WAYBILL_INFO(5, " 面单信息不可辨认 "),
    ONLY_PACKAGE_IN_PHOTO(6, " 照片中只有包裹 "),
    NO_PACKAGE_IN_PHOTO(7, " 照片中无包裹 "),
    PACKAGE_TOO_LARGE(8, " 包裹尺寸放不进信箱 "),
    PHOTO_INFO_MISMATCH(9, " 照片与运单信息不符 "),
    PACKAGE_NOT_IN_MAILBOX(10, " 包裹未放进信箱 "),
    NO_POD(11, " 没有 POD"),
    WRONG_DELIVERY_ADDRESS(100, " 送错地址 "),
    NO_CLEAR_ADDRESS_INFO(101, " 无清晰 / 无门牌号 / 街道号 / 门牌号 "),
    PLACED_IN_DANGEROUS_AREA(102, " 放在危险 / 公共区域 "),
    PACKAGE_AT_COLLECTION_POINT(103, "POBOX/USPS/UPS 代收点 "),
    NO_OR_INCORRECT_WAYBILL_PHOTO(104, " 无面单照片 / 面单不符 / 面单不清晰 "),
    UNCLEAR_OR_IRRELEVANT_PHOTO(105, " 全黑 / 不清晰 / 跟签收地址无关 "),
    NO_PACKAGE_LOCATION_INFO(200, " 无包裹放置位置 / 照片无包裹 "),
    PACKAGE_IN_MAILBOX(201, " 放在邮箱里 "),
    RECIPIENT_PERSONAL_INFO_SHOWN(202, " 拍摄收件人面部 / 身份证 / 照片 / 指纹 ");

    private final int code;
    private final String description;

    public static NonComplianceReason getByCode(int code) {
        for (NonComplianceReason reason : values()) {
            if (reason.getCode() == code) {
                return reason;
            }
        }
        throw new IllegalArgumentException("No matching NonComplianceReason found for code: " + code);
    }
}