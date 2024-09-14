package org.ipring.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author: lgj
 * @date: 2022/12/06 9:53
 * @description:
 */
public abstract class PwdUtil {

    @RequiredArgsConstructor
    @Getter
    public enum PwdType {

        LOW_CHAR(1, "小写字符", "abcdefghijklmnopqrstuvwxyz"),
        UPPER_CHAR(2, "大写字符", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        NUM_CHAR(3, "数字", "0123456789"),
        ;

        private final int code;
        private final String desc;
        private final String source;

        private static final Map<Integer, String> PWD_CODE_2_SOURCE =
                Arrays.stream(PwdType.values()).collect(Collectors.toMap(PwdType::getCode, PwdType::getSource));

        /**
         * 直接获取某种密码范围
         */
        public static char getRandomChar(int code) {
            final String source = PWD_CODE_2_SOURCE.getOrDefault(code, LOW_CHAR.getDesc());
            final int randomNum = ThreadLocalRandom.current().nextInt(source.length());
            return source.charAt(randomNum);
        }

        /**
         * 随机
         */
        public static char getRandomChar() {
            final int ranNum = ThreadLocalRandom.current().nextInt(1, PWD_CODE_2_SOURCE.size() + 1);
            return getRandomChar(ranNum);
        }
    }

    private static final int PWD_INIT_LENGTH = 10; // 密码初始给到10位 8位+2位(给大小写 产品需要一定有两种随机)

    /**
     * 随机密码
     *
     * @return 密码
     */
    public static String randomPwd() {
        StringBuilder sb = new StringBuilder();
        /*
         * 先初始化一个大写 一个小写的两位 下面拼接的时候记得减去2
         */
        char upChar = PwdType.getRandomChar(PwdType.UPPER_CHAR.getCode());
        char lowChar = PwdType.getRandomChar(PwdType.LOW_CHAR.getCode());

        sb.append(upChar);
        sb.append(lowChar);
        for (int i = 0; i < PWD_INIT_LENGTH - 2; i++) {
            sb.append(PwdType.getRandomChar());
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(randomPwd());
        }
    }
}
