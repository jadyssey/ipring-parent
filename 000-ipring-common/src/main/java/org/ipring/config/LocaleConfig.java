package org.ipring.config;

import org.ipring.enums.common.LangTypeStr;
import org.ipring.util.HttpUtils;
import org.ipring.util.MessageUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.util.Locale;

/**
 * 多语言配置
 *
 * @author LiuGuansheng
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver(MessageSource messageSource) {
        Locale.setDefault(MessageUtils.createLocale(LangTypeStr.EN_US.getType()));
        MessageUtils.setMessageSource(messageSource);
        return new LangLocaleResolver();
    }

    /**
     * 自定义语言解析器
     *
     * @author LiuGuansheng
     */
    private static class LangLocaleResolver implements LocaleResolver {

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            return MessageUtils.createLocale(HttpUtils.getLangStr());
        }

        /**
         * 设置默认语言
         *
         * @param request
         * @param response
         * @param locale
         */
        @Override
        public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        }
    }

    @Resource
    MessageSource messageSource;

    @Bean
    public Validator getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
