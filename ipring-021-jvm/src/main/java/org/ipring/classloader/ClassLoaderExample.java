package org.ipring.classloader;

public class ClassLoaderExample {
    public static void main(String[] args) throws Exception {
        // 创建两个不同的类加载器
        ClassLoader classLoader1 = new CustomClassLoader();
        ClassLoader classLoader2 = new CustomClassLoader();

        // 使用不同的类加载器加载相同的类
        Class<?> class1 = classLoader1.loadClass("org.ipring.classloader.MyClass");
        Class<?> class2 = classLoader2.loadClass("org.ipring.classloader.MyClass");

        // 打印两个类的加载器以及是否相等
        System.out.println("Class 1 Loader: " + class1.getClassLoader());
        System.out.println("Class 2 Loader: " + class2.getClassLoader());
        System.out.println("Are the classes equal? " + (class1 == class2));
    }

    /**
     * 唯一标识一个类：类加载器 + 类全限定名
     *
     * 加载逻辑：
     * loadClass方法会检查该类是否已经被加载过，如果已加载则直接返回对应的Class对象。
     * 如果没有加载过，则调用父ClassLoader的loadClass方法，如果父ClassLoader也无法加载，则调用findClass方法来实际查找和加载类。
     *
     * 注意：
     * 如果自定义类加载器不想违背双亲委派模型，一般只需要重写findClass方法即可，如果想违背双亲委派模型，则还需要重写loadClass方法。
     *
     * 原理：
     * ClassLoader的源码里有一个map，这个map的key是对应的包路径，value是对应的package对象，
     * 所以自定义类加载器、应用类加载器等都自己维护了一个包路径到package对象的映射，等同于每个加载器都有自己的命名空间。
     */
    static class CustomClassLoader extends ClassLoader {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if ("org.ipring.classloader.MyClass".equals(name)) {
                // 加载自定义的类
                return findClass(name);
            }
            // 对于其他类，委托给父类加载器
            return super.loadClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // 实现自定义类加载逻辑，这里简单起见直接返回null
            // 实际应用中，需要从特定的地方（例如文件、数据库）加载类的字节码并定义类
            return null;
        }
    }
}
