package org.ipring.util;

import org.ipring.enums.common.LangTypeInt;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 获取语言国际化工具类
 *
 * @author lgj
 * @date 8/2/2023
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageUtils {

    private static MessageSource messageSource;

    /**
     * 在初始化阶段设置
     *
     * @param messageSource
     */
    public static void setMessageSource(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    /**
     * 获取单个国际化翻译值
     */
    public static String getMsg(String msgKey) {
        return getMsg(msgKey, LocaleContextHolder.getLocale());
    }

    /**
     * 获取指定语言的message
     *
     * @param key
     * @param lang
     * @param args
     * @return
     */
    public static String getMsg(String key, Integer lang, Object... args) {
        return getMsg(key, LangTypeInt.getLangTypeOrDefault(lang, LangTypeInt.EN_US).getCode(), args);
    }

    /**
     * 指定获取多语言message
     *
     * @param key
     * @param lang
     * @param args
     * @return
     */
    private static String getMsg(String key, String lang, Object... args) {
        return getMsg(key, createLocale(lang), args);
    }

    /**
     * 获取指定地区的国际化翻译
     *
     * @param msgKey 消息key
     * @param locale 地区
     * @return
     */
    private static String getMsg(String msgKey, Locale locale, Object... args) {
        if (StringUtils.isBlank(msgKey)) {
            return msgKey;
        }
        try {
            return messageSource.getMessage(msgKey, args, locale);
        } catch (NoSuchMessageException e) {
            log.error("Error get i18n message，message：{}", e.getLocalizedMessage());
            return msgKey;
        }
    }

    /**
     * 生成Locale对象
     *
     * @param localeStr
     * @return
     */
    public static Locale createLocale(String localeStr) {
        Locale locale = Locale.getDefault();
        if (StringUtils.isNumeric(localeStr)) {
            log.error("语言入参错误: {}", localeStr);
            return locale;
        }
        if (StringUtils.isNotBlank(localeStr)) {
            String[] language = localeStr.split("_");
            locale = new Locale(language[0], language[1]);
        }
        return locale;
    }
}


