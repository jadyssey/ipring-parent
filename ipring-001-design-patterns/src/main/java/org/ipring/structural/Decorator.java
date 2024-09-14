package org.ipring.structural;

import org.ipring.structural.decorator.*;

/**
 * 装饰者模式
 *
 * @Author lgj
 * @Date 2024/4/24
 */
public class Decorator {
    /*
    Java 核心程序库中有一些关于装饰的示例：

    java.io.InputStream、 OutputStream、 Reader 和 Writer 的所有代码都有以自身类型的对象作为参数的构造函数。

    java.util.Collections； checkedXXX()、 synchronizedXXX() 和 unmodifiableXXX() 方法。

    javax.servlet.http.HttpServletRequestWrapper 和 HttpServletResponseWrapper

    识别方法： 装饰可通过以当前类或对象为参数的创建方法或构造函数来识别。
     */
    public static void main(String[] args) {
        String salaryRecords = "Name,Salary\nJohn Smith,100000\nSteven Jobs,912000";
        // 加密 压缩
        DataSourceDecorator encoded = new CompressionDecorator(
                new EncryptionDecorator(
                        new FileDataSource("D:/OutputDemo.txt")));
        encoded.writeData(salaryRecords);

        DataSource plain = new FileDataSource("D:/OutputDemo.txt");

        System.out.printf("Input -> ", salaryRecords);
        System.out.printf("Encoded -> ", plain.readData());
        System.out.printf("Decoded -> ", encoded.readData());
    }
}

