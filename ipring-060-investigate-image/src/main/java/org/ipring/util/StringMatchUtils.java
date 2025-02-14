package org.ipring.util;

public class StringMatchUtils {

    /**
     * 利用动态规划算法计算两个字符串的编辑距离
     * 这个距离表示将 str1 转换为 str2 所需要的最少操作数（插入、删除、替换）。
     *
     * @param str1
     * @param str2
     * @return
     */
    private static int levenshteinDistance(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 根据编辑距离和较长字符串的长度计算两个字符串的匹配率
     *
     * @param str1
     * @param str2
     * @return
     */
    public static double matchingRate(String str1, String str2) {
        int distance = levenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        double matchRate = (1 - (double) distance / maxLength) * 100;
        // 保留一位小数
        return Math.round(matchRate * 10.0) / 10.0;
    }

    public static void main(String[] args) {
        String str1 = "GF5091236880004";
        String str2 = "GF50912336880004";
        double rate = matchingRate(str1, str2);
        System.out.printf("字符串 \"%s\" 相较于 \"%s\" 的匹配率为: %.2f%%\n", str1, str2, rate);
    }
}