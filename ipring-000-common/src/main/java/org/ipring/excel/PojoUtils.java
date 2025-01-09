package org.ipring.excel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

/**
 * 操作 bean 的工具类.
 *
 * @author YuanWenkai
 * @date 2020/7/1
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PojoUtils {

    /**
     * 复制属性到目标实体，来源中为 null 的属性会被忽略
     *
     * @param source 复制来源
     * @param target 复制目标
     */
    public static void copyPropertiesIgnoreNull(Object source, Object target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 复制属性到目标实体，来源中为空的属性会被忽略
     *
     * @param source 复制来源
     * @param target 复制目标
     */
    public static void copyPropertiesIgnoreEmpty(Object source, Object target) {
        BeanUtils.copyProperties(source, target, getEmptyPropertyNames(source));
    }

    /**
     * 利用类无参构造方法生成对象
     *
     * @throws IllegalStateException 无法生成对象时抛出
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("cannot create object from the public no args constructor of class: " + clazz.getName());
        }
    }

    /**
     * 得到实体中为 null 的属性名称数组
     */
    private static String[] getNullPropertyNames(Object bean) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] descriptors = beanWrapper.getPropertyDescriptors();
        return Stream.of(descriptors).map(FeatureDescriptor::getName)
                .filter(name -> beanWrapper.getPropertyValue(name) == null)
                .distinct().toArray(String[]::new);
    }

    /**
     * 得到实体中为空的属性名称数组
     */
    private static String[] getEmptyPropertyNames(Object bean) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
        PropertyDescriptor[] descriptors = beanWrapper.getPropertyDescriptors();
        return Stream.of(descriptors).map(FeatureDescriptor::getName)
                .filter(name -> StringUtils.isEmpty(beanWrapper.getPropertyValue(name)))
                .distinct().toArray(String[]::new);
    }
}
