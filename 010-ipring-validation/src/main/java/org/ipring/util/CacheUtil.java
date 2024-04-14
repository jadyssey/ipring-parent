package org.ipring.util;

import javax.validation.constraints.NotBlank;

/**
 * @Author lgj
 * @Date 2024/4/14
 */
public class CacheUtil {
    public static void get(@NotBlank String key) {
        System.out.println("CacheUtil get key = " + key);
    }
}
