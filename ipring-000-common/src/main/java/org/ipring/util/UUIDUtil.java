package org.ipring.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * @author ly
 * @date 2022/2/8
 */
public abstract class UUIDUtil {

    @RequiredArgsConstructor
    @Getter
    public enum UUIDLenEnum {
        ULTRA(32),
        UL8(8),
        ;
        private final int len;
    }

    public static final String UUID_DEFAULT_DELIMITER = "-";

    public static final String UUID_DELIMITER = "";

    /**
     * 获取一个指定长度uuid字符串 去掉分隔符
     */
    public static String getDecLenUuidStr(UUIDLenEnum len) {
        return UUID.randomUUID().toString().replaceAll(UUID_DEFAULT_DELIMITER, UUID_DELIMITER).substring(0, len.getLen());
    }

    /**
     * 获取一个指定长度uuid字符串 去掉分隔符 全小写
     */
    public static String getLcDecLenUuidStr(UUIDLenEnum len) {
        return getDecLenUuidStr(len).toLowerCase();
    }

    /**
     * 获取一个全长长度uuid字符串 去掉分隔符 全小写
     */
    public static String getLcDefaultLenUuidStr() {
        return getLcDecLenUuidStr(UUIDLenEnum.ULTRA);
    }

    public static String generateTraceId() {
        return getLcDecLenUuidStr(UUIDLenEnum.UL8);
    }

}
