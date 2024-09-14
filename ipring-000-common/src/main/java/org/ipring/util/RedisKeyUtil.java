package org.ipring.util;

import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * @author: lgj
 * @date: 2024/04/02 18:52
 * @description:
 */
public class RedisKeyUtil {

    public static String accountKey(Long accountId) {
        return "account:" + accountId;
    }


    /**
     * 账号下单并发锁
     *
     * @param accountId
     * @return
     */
    public static String accountLock(Long accountId) {
        return "lock:order:" + accountId;
    }

    /**
     * 获取简单的key
     *
     * @param key key
     * @return 返回key
     */
    public static String getKey(String key) {
        return getKey(key, null);
    }

    /**
     * 根据前缀和传入参数自动生成key
     *
     * @param keyPrefix
     * @param id
     * @param <ID>
     * @return
     */
    public static <ID> String getKey(String keyPrefix, ID id) {
        if (id == null) {
            return keyPrefix;
        }
        String key = "";
        //简单数据类型与简单字符串
        if (TypeConversion.isSimpleType(id)) {
            key = String.valueOf(id);
        } else {
            key = convertModelToString(id);
        }
        if (!StringUtils.hasText(key)) {
            key = "";
        }
        return keyPrefix.concat(key);
    }


    /**
     * 将模型转为字符串，用于redis的key
     *
     * @param model
     * @return
     */
    public static String convertModelToString(Object model) {
        Class<?> clazz = model.getClass();
        StringBuilder builder = new StringBuilder();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object value;
            try {
                value = field.get(model);
            } catch (IllegalAccessException e) {
                value = null;
            }
            builder.append(fieldName).append(":").append(value).append(":");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static String createAccountLock(Long userId) {
        return "account_create:" + userId;
    }
}
