package org.ipring.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ipring.enums.StrEnumType;

import java.util.Locale;

/**
 * @author: panxuan
 * @date: 2020/05/25 11:15
 * @description: 语言类型枚举值
 */
@Getter
@AllArgsConstructor
public enum LangTypeStr implements StrEnumType {
    /**
     * 语言类型枚举值
     */
    ERROR("error", "未知语言"),
    ZH_CN("zh_cn", "简体中文"),
    ZH_TW("zh_tw", "繁体中文"),
    EN_US("en_us", "英文"),
    AR_AE("ar_ae", "阿拉伯语"),
    FR_FR("fr_fr", "法文"),
    HI_IN("hi_in", "印地语"),
    KO_KR("ko_kr", "韩语"),
    MS_MY("ms_my", "马来语"),
    TH_TH("th_th", "泰语"),
    VI_VN( "vi_vn", "越南语"),
    RU_RU( "ru_ru", "俄语"),
    IN_ID( "in_id", "印尼语"),
    JA_JP( "ja_jp", "日语"),
    ES_ES( "es_es", "西班牙语"),
    PT_PT("pt_pt", "葡萄牙语"),
    IT_IT("it_it", "意大利语"),
    TR_TR("tr_tr", "土耳其"),
    ;

    private final String type;

    private final String description;

    public static void main(String[] args) {

        final Locale locale1 = new Locale("zh-cn");
        final Locale locale2 = new Locale("zh-CN");
        System.out.println(locale1);
        System.out.println(locale2);
    }
}
