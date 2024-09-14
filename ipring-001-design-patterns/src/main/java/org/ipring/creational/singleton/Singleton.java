package org.ipring.creational.singleton;

/**
 * @Author lgj
 * @Date 2024/4/24
 */
/*
单例模式：
java.lang.Runtime#getRuntime()
java.awt.Desktop#getDesktop()
java.lang.System#getSecurityManager()
识别方法： 单例可以通过返回相同缓存对象的静态构建方法来识别。
 */
public class Singleton {
    private static Singleton instance;
    public String value;

    // 私有化构造函数
    private Singleton(String value) {
        this.value = value;
    }

    // 实现一个静态的构建方法
    public static Singleton getInstance(String value) {
        if (instance == null) {
            instance = new Singleton(value);
        }
        return instance;
    }
}
