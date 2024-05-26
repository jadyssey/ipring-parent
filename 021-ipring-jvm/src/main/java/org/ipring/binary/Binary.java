package org.ipring.binary;

/**
 * @Author lgj
 * @Date 2024/5/26
 */
public class Binary {
    public static void main(String[] args) {
        /*
        在二进制补码表示法中，负数是通过对该数的绝对值取反（即每个位取反）然后加 1 来表示的：

        以 8 位二进制为例，来看 -1 的表示方法：
        1 的二进制是 00000001。
        对每一位取反得到 11111110。
        加 1 得到 11111111。
        所以，-1 在 8 位二进制补码表示法中的表示是 11111111。
         */
        if (-1 == (~1 + 1)) {
            System.out.println(true);
        }
    }
}
