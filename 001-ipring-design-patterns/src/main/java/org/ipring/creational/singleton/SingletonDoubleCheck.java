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
public class SingletonDoubleCheck {
    private static SingletonDoubleCheck instance;
    public String value;

    // 私有化构造函数
    private SingletonDoubleCheck(String value) {
        this.value = value;
    }

    // 实现一个静态的构建方法
    public static SingletonDoubleCheck getInstance(String value) {
        if (instance == null) {
            // 加锁再次空校验，双重检测锁
            synchronized (SingletonDoubleCheck.class) {
                if (instance == null) {
                    instance = new SingletonDoubleCheck(value);
                }
            }
        }
        return instance;
    }
}
