package org.ipring.enums.common;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author: panxuan
 * @date: 2020/05/25 11:15
 * @description: 语言类型枚举值
 */
@Getter
@AllArgsConstructor
public enum LangTypeInt implements IntEnumType {
    /**
     * 语言类型枚举值
     */
    ERROR(0, "error", "未知语言"),
    ZH_CN(12, "zh_cn", "简体中文"),
    ZH_TW(2, "zh_tw", "繁体中文"),
    EN_US(3, "en_us", "英文"),
    AR_AE(4, "ar_ae", "阿拉伯语"),
    FR_FR(5, "fr_fr", "法文"),
    HI_IN(6, "hi_in", "印地语"),
    KO_KR(7, "ko_kr", "韩语"),
    MS_MY(8, "ms_my", "马来语"),
    TH_TH(9, "th_th", "泰语"),
    VI_VN(10, "vi_vn", "越南语"),
    RU_RU(11, "ru_ru", "俄语"),
    IN_ID(13, "in_id", "印尼语"),
    JA_JP(14, "ja_jp", "日语"),
    ES_ES(15, "es_es", "西班牙语"),
    PT_PT(16, "pt_pt", "葡萄牙语"),
    IT_IT(17, "it_it", "意大利语"),
    TR_TR(18, "tr_tr", "土耳其"),
    ;

    public static LangTypeInt getLangType(Integer lang) {
        if (Objects.isNull(lang)) {
            return null;
        }
        return ALL_LANG_MAP.get(lang);
    }

    public static LangTypeInt getLangTypeOrDefault(Integer lang, LangTypeInt defaultLang) {
        return Optional.ofNullable(getLangType(lang)).orElse(defaultLang);
    }

    public static Integer getLangByCode(String langCode) {
        if (StringUtils.hasLength(langCode)) {
            return STR_TO_INT_MAP.get(langCode.toLowerCase());
        }
        return null;
    }

    private static final Map<Integer, LangTypeInt> ALL_LANG_MAP = new HashMap<>();
    private static final Map<String, Integer> STR_TO_INT_MAP = new HashMap<>();
    public static final List<Integer> LANG_USE_LIST = new ArrayList<>();
    public static final Map<Integer, String> LANG_URL_MAP = new HashMap<>();

    public String LangToBaseService() {
        return LANG_TO_BASE_SERVICE.get(this.type);
    }

    // 基础服务请求头语言格式
    public static final Map<Integer, String> LANG_TO_BASE_SERVICE = new HashMap<>();

    public static final Map<Integer, String> TRANSLATE_GOOGLE_MAP = new HashMap<>(8);

    public static final Map<Integer, String> CONVERT_FAST_BULL_MAP = new HashMap<>();
    static {
        for (LangTypeInt lang : LangTypeInt.values()) {
            STR_TO_INT_MAP.put(lang.getCode(), lang.getType());
            if (ERROR.equalTo(lang.getType())) continue;
            ALL_LANG_MAP.put(lang.getType(), lang);
        }

        //谷歌翻译对应语言
        TRANSLATE_GOOGLE_MAP.put(ZH_CN.type, "zh-CN");
        TRANSLATE_GOOGLE_MAP.put(ZH_TW.type, "zh-TW");
        TRANSLATE_GOOGLE_MAP.put(EN_US.type, "en");
        TRANSLATE_GOOGLE_MAP.put(AR_AE.type, "ar");
        TRANSLATE_GOOGLE_MAP.put(FR_FR.type, "fr");
        TRANSLATE_GOOGLE_MAP.put(HI_IN.type, "hi");
        TRANSLATE_GOOGLE_MAP.put(KO_KR.type, "ko");
        TRANSLATE_GOOGLE_MAP.put(MS_MY.type, "ms");
        TRANSLATE_GOOGLE_MAP.put(TH_TH.type, "th");
        TRANSLATE_GOOGLE_MAP.put(VI_VN.type, "vi");
        TRANSLATE_GOOGLE_MAP.put(RU_RU.type, "ru");
        TRANSLATE_GOOGLE_MAP.put(JA_JP.type, "ja");
        TRANSLATE_GOOGLE_MAP.put(IN_ID.type, "id");
        TRANSLATE_GOOGLE_MAP.put(ES_ES.type, "es");
        TRANSLATE_GOOGLE_MAP.put(PT_PT.type, "pt");
        TRANSLATE_GOOGLE_MAP.put(IT_IT.type, "it");
        TRANSLATE_GOOGLE_MAP.put(TR_TR.type, "tr");

        LANG_TO_BASE_SERVICE.put(ZH_CN.type, "zh-CN");
        LANG_TO_BASE_SERVICE.put(ZH_TW.type, "zh-TW");
        LANG_TO_BASE_SERVICE.put(EN_US.type, "en-US");
        LANG_TO_BASE_SERVICE.put(AR_AE.type, "ar-AE");
        LANG_TO_BASE_SERVICE.put(FR_FR.type, "fr-FR");
        LANG_TO_BASE_SERVICE.put(HI_IN.type, "hi-IN");
        LANG_TO_BASE_SERVICE.put(KO_KR.type, "ko-KR");
        LANG_TO_BASE_SERVICE.put(MS_MY.type, "ms-MY");
        LANG_TO_BASE_SERVICE.put(TH_TH.type, "th-TH");
        LANG_TO_BASE_SERVICE.put(VI_VN.type, "vi-VN");
        LANG_TO_BASE_SERVICE.put(RU_RU.type, "ru-RU");
        LANG_TO_BASE_SERVICE.put(IN_ID.type, "id-ID");
        LANG_TO_BASE_SERVICE.put(JA_JP.type, "ja-JP");
        LANG_TO_BASE_SERVICE.put(ES_ES.type, "es-ES");
        LANG_TO_BASE_SERVICE.put(PT_PT.type, "pt-PT");
        LANG_TO_BASE_SERVICE.put(IT_IT.type, "it-IT");
        LANG_TO_BASE_SERVICE.put(TR_TR.type, "tr-TR");

        // 请不要更换这个语言的顺序
        LANG_USE_LIST.add(EN_US.type);
        LANG_USE_LIST.add(AR_AE.type);
        LANG_USE_LIST.add(ZH_CN.type);
        LANG_USE_LIST.add(ZH_TW.type);
        LANG_USE_LIST.add(MS_MY.type);
        LANG_USE_LIST.add(TH_TH.type);
        LANG_USE_LIST.add(VI_VN.type);
        LANG_USE_LIST.add(ES_ES.type);
        LANG_USE_LIST.add(PT_PT.type);
        LANG_USE_LIST.add(TR_TR.type);
        LANG_USE_LIST.add(IT_IT.type);
        LANG_USE_LIST.add(FR_FR.type);
        LANG_USE_LIST.add(RU_RU.type);
        LANG_USE_LIST.add(JA_JP.type);
        LANG_USE_LIST.add(KO_KR.type);

        LANG_URL_MAP.put(ZH_CN.type, "cn");
        LANG_URL_MAP.put(ZH_TW.type, "tw");
        LANG_URL_MAP.put(EN_US.type, "en");
        LANG_URL_MAP.put(AR_AE.type, "ar");
        LANG_URL_MAP.put(MS_MY.type, "ms");
        LANG_URL_MAP.put(TH_TH.type, "th");
        LANG_URL_MAP.put(VI_VN.type, "vn");
        LANG_URL_MAP.put(IN_ID.type, "id");
        LANG_URL_MAP.put(ES_ES.type, "es");
        LANG_URL_MAP.put(PT_PT.type, "pt");
        LANG_URL_MAP.put(TR_TR.type, "tr");
        LANG_URL_MAP.put(IT_IT.type, "it");
        LANG_URL_MAP.put(FR_FR.type, "fr");
        LANG_URL_MAP.put(RU_RU.type, "ru");
        LANG_URL_MAP.put(JA_JP.type, "jp");
        LANG_URL_MAP.put(KO_KR.type, "ko");


        CONVERT_FAST_BULL_MAP.put(EN_US.type, "0");
        CONVERT_FAST_BULL_MAP.put(ZH_CN.type, "1");
        CONVERT_FAST_BULL_MAP.put(ZH_TW.type, "2");
        CONVERT_FAST_BULL_MAP.put(RU_RU.type, "3");
        CONVERT_FAST_BULL_MAP.put(FR_FR.type, "4");
        CONVERT_FAST_BULL_MAP.put(KO_KR.type, "5");
        CONVERT_FAST_BULL_MAP.put(JA_JP.type, "6");
        CONVERT_FAST_BULL_MAP.put(TH_TH.type, "7");
        CONVERT_FAST_BULL_MAP.put(AR_AE.type, "8");
        CONVERT_FAST_BULL_MAP.put(IT_IT.type, "9");
        CONVERT_FAST_BULL_MAP.put(VI_VN.type, "10");
        CONVERT_FAST_BULL_MAP.put(ES_ES.type, "11");
        //CONVERT_FAST_BULL_MAP.put("国内简中", "12");
        CONVERT_FAST_BULL_MAP.put(IN_ID.type, "13");
        CONVERT_FAST_BULL_MAP.put(MS_MY.type, "14");
        CONVERT_FAST_BULL_MAP.put(PT_PT.type, "15");
        CONVERT_FAST_BULL_MAP.put(TR_TR.type, "16");
    }

    private final Integer type;

    private final String code;

    private final String description;
}
