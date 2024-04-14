package org.ipring.anno;


import org.ipring.anno.impl.IntEnumValidImpl;
import org.ipring.anno.impl.StringEnumValidImpl;
import org.ipring.enums.EnumType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * 自定义枚举校验注解
 *
 * @author lgj
 * @date 8/2/2023
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(value = RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {IntEnumValidImpl.class, StringEnumValidImpl.class})
public @interface EnumValue {

    /**
     * 提示信息
     *
     * @return
     */
    String message() default "枚举类参数错误";

    /**
     * 校验枚举类
     */
    Class<? extends EnumType<?>> type();

    /**
     * 是否需要校验（如果单纯只是想作为swagger提示展示的时候就设置为false，此时不会进行枚举校验）
     * <p>
     * 注意 int类型：0在最后会当成空值校验 走可否为空逻辑
     */
    boolean doValid() default true;

    /**
     * 是否允许为空
     */
    boolean nullable() default true;

    /**
     * 组
     *
     * @return
     */
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
