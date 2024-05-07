package org.ipring;

import java.time.Instant;

/**
 * @author lgj
 * @date 2024/5/6
 **/
public class Test {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1_000_000; i++) {
            long time = Instant.now().toEpochMilli();
            //long time = System.currentTimeMillis();
        }
        long end = System.currentTimeMillis();
        System.out.println("end = " + (end - start));
    }
}
