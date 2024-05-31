package org.ipring.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: Rainful
 * @date: 2024/04/18 14:04
 * @description:
 */
@Slf4j
public abstract class TokenGenUtil {

    public static <T> String getTokenByEle(T uid) {
        return SecureUtil.aesEn(String.valueOf(uid));
    }

    public static <T, E> String getTokenByEle(T uid, E secret) {
        return MD5Utils.md5(String.valueOf(uid) + secret);
    }

    public static void main(String[] args) {

        System.out.println(MD5Utils.md5(String.valueOf(211884)));
        final String token = TokenGenUtil.getTokenByEle(2);
        log.info("token:{}", token);

        final String deToken = SecureUtil.aesDe("JsLmeEo6pO+fQVWX0HvB9A==");
        log.info("deToken:{}", deToken);
    }
}
