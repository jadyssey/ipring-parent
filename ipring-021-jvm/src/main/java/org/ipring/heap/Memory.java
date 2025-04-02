package org.ipring.heap;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试内存泄漏代码
 */
public class Memory {

    public static void main(String[] args) {
        int size = 1024 * 1024 * 8; // 1MB=1024KB=1024*1024B=1024*1024*8b
        List<byte[]> list = new ArrayList<byte[]>();
        for (int i = 0; i < 1024; i++) {
            System.out.println("JVM 写入数据" + (i + 1) + "M");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(new byte[size]);  // 每一秒就往堆中写入一个数据
        }

    }

}