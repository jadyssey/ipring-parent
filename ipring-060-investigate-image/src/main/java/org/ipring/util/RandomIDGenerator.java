package org.ipring.util;

public class RandomIDGenerator {
    public static void main(String[] args) {
        // 调用生成方法并打印结果
        String randomID = generateRandomID();
        System.out.println(randomID);
    }

    public static String generateRandomID() {
        // 创建StringBuilder用于构建最终编号
        StringBuilder sb = new StringBuilder();

        // 生成两个随机大写字母
        for (int i = 0; i < 2; i++) {
            char randomLetter = (char) ('A' + Math.random() * ('Z' - 'A' + 1));
            sb.append(randomLetter);
        }

        // 生成13位随机数字
        long randomNumber = (long) (Math.random() * (9999999999999L - 1000000000000L + 1)) + 1000000000000L;
        sb.append(randomNumber);

        // 返回生成的编号
        return sb.toString();
    }
}