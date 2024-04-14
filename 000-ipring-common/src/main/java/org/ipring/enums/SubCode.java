package org.ipring.enums;

import org.ipring.anno.StlServiceCode;
import org.ipring.exception.ServiceException;
import org.ipring.util.MessageUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * subCode枚举类的顶级接口
 *
 * @author lgj
 * @date 8/2/2023
 */
public interface SubCode {

    /**
     * 错误代码
     *
     * @return
     */
    String getCode();

    /**
     * 获取完整subCode值
     *
     * @return
     */
    @SneakyThrows
    default String getSubCode() {
        Class<? extends SubCode> subClass = this.getClass();

        // 获取外部类StlServiceCode
        Class<?> outerClass  = subClass.getEnclosingClass();
        Assert.isTrue(outerClass != null, "找不到外部类");
        StlServiceCode stlServiceCode = outerClass.getAnnotation(StlServiceCode.class);
        Assert.isTrue(stlServiceCode != null, "外部类缺少StlServiceCode注解");

        return stlServiceCode.code().getType() + this.getCode();
    }

    /**
     * 获取subCode描述信息
     *
     * @return
     */
    String getDesc();

    /**
     * 获取多语言key
     *
     * @return
     */
    String getI18nKey();


    /**
     * 获取当前枚举值的名称
     *
     * @return 枚举值名称
     */
    default String getName() {
        return this.toString();
    }

    /**
     * 抛出异常
     *
     * @return
     */
    default ServiceException exception() {
        return new ServiceException(this);
    }

    /**
     * 获取国际化提示语
     *
     * @return
     */
    default String getI18nMsg() {
        String i18nKey = getI18nKey();
        return StringUtils.isNotBlank(i18nKey) ? MessageUtils.getMsg(i18nKey) : getDesc();
    }

    /**
     * 获取swagger描述
     *
     * @return
     */
    default String getSwaggerMsg() {
        return getSubCode() + "：" + getDesc();
    }
}
