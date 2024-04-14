package org.ipring.anno.impl;

import org.ipring.anno.EnumValue;
import org.ipring.enums.EnumType;
import org.ipring.util.EnumValueUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 自定义枚举校验规则
 *
 * @author lgj
 * @date 8/2/2023
 */
@Component
public class StringEnumValidImpl implements ConstraintValidator<EnumValue, String> {

    private EnumValue constraintAnnotation;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (!constraintAnnotation.doValid()) {
            return true;
        }
        if (value == null) {
            return constraintAnnotation.nullable();
        }
        Class<? extends EnumType<?>> cls = this.constraintAnnotation.type();
        return EnumValueUtils.contains(cls, value);
    }
}
