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
public class IntEnumValidImpl implements ConstraintValidator<EnumValue, Integer> {

    private EnumValue constraintAnnotation;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext constraintValidatorContext) {
        if (!constraintAnnotation.doValid()) {
            return true;
        }
        if (value == null) {
            return constraintAnnotation.nullable();
        }
        Class<? extends EnumType<?>> cls = this.constraintAnnotation.type();
        if (EnumValueUtils.contains(cls, value)) {
            return true;
        }
        //数字类型数据，考虑到前端可能使用的基本类型，默认值为0，所以0在最后当成空值校验可否为空
        if (0 == value) {
            return constraintAnnotation.nullable();
        }
        return false;
    }
}
