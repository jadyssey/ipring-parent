package org.ipring.anno;

import org.ipring.constant.CommonConstants;
import org.ipring.enums.SubCode;
import org.ipring.enums.common.ResponseContainerType;
import io.swagger.annotations.ResponseHeader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口swagger增强注解，作用同{@link io.swagger.annotations.ApiOperation}
 *
 * @author lgj
 * @date 8/2/2023
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StlApiOperation {
    /**
     * 接口标题，同{@link io.swagger.annotations.ApiOperation}的value字段，两个字段可同时使用
     *
     * @return
     */
    String title() default "";

    /**
     * 标题提示，同{@link io.swagger.annotations.ApiOperation}的notes字段
     */
    String tip() default "<B style='color:#49cc90'>subCode说明：</B>";

    /**
     * subCode类型，指明SubCode的具体对象
     *
     * @return
     */
    Class<? extends SubCode>[] subCodeType() default SubCode.class;

    /**
     * subCode前缀，会根据startWith的规则匹配
     *
     * @return
     */
    String[] codePrefix() default CommonConstants.NONE;

    /**
     * 显示时需要排除的subCode
     *
     * @return
     */
    String[] excludeSubCode() default "";
    /**
     * 返回类型
     *
     * @return
     */
    Class<?> response() default Void.class;

    /**
     * 声明包装集合的容器
     *
     * @return
     * @see ResponseContainerType
     */
    ResponseContainerType responseContainer() default ResponseContainerType.NONE;


    /**
     * 返回头
     *
     * @return
     */
    ResponseHeader[] responseHeaders() default @ResponseHeader();
}
