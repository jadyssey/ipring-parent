package org.ipring.string;

import java.util.Optional;

/**
 * @author lgj
 * @date 2024/5/11
 **/
public class StringTest {
    public static void main(String[] args) {
        String a = "a";
        int b = a.compareTo("b");// 从头到尾按ascii码顺序排列字符串

        Integer c = null;
        Integer d = Optional.ofNullable(c).orElseGet(() -> b);
        System.out.println("d = " + d);
    }
}
