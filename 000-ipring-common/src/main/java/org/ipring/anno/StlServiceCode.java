package org.ipring.anno;


import org.ipring.enums.common.ServiceSubCodeStr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lgj
 * @date 8/2/2023
 * @description: 管理subcode，code值用于拼接到subcode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface StlServiceCode {

    ServiceSubCodeStr code();

    String description() default "";
}
