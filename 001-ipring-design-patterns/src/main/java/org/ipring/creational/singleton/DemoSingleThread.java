package org.ipring.creational.singleton;

/**
 * @Author lgj
 * @Date 2024/4/25
 */
public class DemoSingleThread {
    public static void main(String[] args) {
        Singleton instance1 = Singleton.getInstance("instance 1");
        Singleton instance2 = Singleton.getInstance("instance 2");

        System.out.println("instance1 value = " + instance1.value);
        System.out.println();
        System.out.println("instance2 value = " + instance2.value);
    }
}
