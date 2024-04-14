package org.ipring.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * json工具类
 *
 * @author lgj
 * @date 8/2/2023
 */
@Component
public class JsonUtils implements ApplicationContextAware {

    private static ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        objectMapper = applicationContext.getBean(ObjectMapper.class);
    }

    /**
     * 将任意对象转变为json字符串
     */
    public static <T> String toJson(T object) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return (String) object;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error(e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * 将一个对象数据复制到另一个对象中
     *
     * @param object 源对象
     * @param clazz  目标对象的Class
     * @param <T>    目标对象泛型
     * @param <E>    源对象泛型
     * @return
     */
    public static <T, E> T copyObject(E object, Class<T> clazz) {
        String json = toJson(object);
        T returnObj = null;
        try {
            returnObj = objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
        return returnObj;
    }

    /**
     * 将任意对象转变为map
     */
    public static <T> Map<String, Object> toMap(T object) {
        Map<String, Object> map = null;
        try {
            String json = toJson(object);
            map = objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
        return map;
    }

    /**
     * 将json字符串转变为任意对象（单一对象）
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T object = null;
        try {
            object = objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("error={}", e.getLocalizedMessage());
        }
        return object;
    }

    public static <T> T toObject(Object object, Class<T> clazz) {
        String json;
        if (object instanceof String) {
            json = (String) object;
        } else {
            json = toJson(object);
        }
        return toObject(json, clazz);
    }

    /**
     * 将map转变为任意对象（单一对象）
     */
    public static <T> T toObject(Map<String, Object> map, Class<T> clazz) {
        T object = null;
        try {
            String json = toJson(map);
            object = objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
        return object;
    }

    /**
     * 将json字符串转变为对象集合（json字符串为对象数组字符串）
     *
     * @param <E>   集合中的元素类型泛型
     * @param clazz 集合元素类型
     * @param json  要转换的json数据
     */
    public static <E> List<E> toList(String json, Class<E> clazz) {
        if (StringUtils.isBlank(json)) return Collections.emptyList();
        List<E> list = null;
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            list = objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
        return list;
    }

    /**
     * 将任意对象转换为MultiValueMap
     *
     * @param object 要转换的对象
     * @param <T>    泛型，对象为任意类型
     * @return
     */
    public static <T> MultiValueMap<String, Object> toMultiValueMap(T object) {
        MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
        Map<String, Object> map = toMap(object);
        if (CollectionUtils.isEmpty(map)) {
            return multiValueMap;
        }
        map.forEach(multiValueMap::add);
        return multiValueMap;
    }

}
