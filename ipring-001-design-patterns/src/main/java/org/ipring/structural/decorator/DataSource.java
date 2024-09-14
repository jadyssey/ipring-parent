package org.ipring.structural.decorator;

/**
 * 程序使用一对装饰来封装数据源对象。 这两个封装器都改变了从磁盘读写数据的方式：
 * 当数据即将被写入磁盘前， 装饰对数据进行加密和压缩。 在原始类对改变毫无察觉的情况下， 将加密后的受保护数据写入文件。
 * 当数据刚从磁盘读出后， 同样通过装饰对数据进行解压和解密。
 *
 * @Author lgj
 * @Date 2024/4/24
 */
public interface DataSource {
    /**
     * 装饰可以改变组件接口所定义的操作
     */

    void writeData(String data);
    String readData();
}
