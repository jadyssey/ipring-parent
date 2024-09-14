package org.ipring.structural.decorator;

import java.util.Base64;

/**
 * 加密装饰
 *
 * @Author lgj
 * @Date 2024/4/24
 */
public class EncryptionDecorator extends DataSourceDecorator {
    public EncryptionDecorator(DataSource wrappee) {
        super(wrappee);
    }
    // 具体装饰必须在被封装对象上调用方法，不过也可以自行在结果中添加一些内容。
    // 装饰必须在调用封装对象之前或之后执行额外的行为。

    @Override
    public void writeData(String data) {
        super.writeData(encode(data));
    }

    private String encode(String data) {
        byte[] bytes = data.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += (byte) 1;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public String readData() {
        return decode(super.readData());
    }

    private String decode(String readData) {
        byte[] bytes = Base64.getDecoder().decode(readData);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] -= (byte) 1;
        }
        return new String(bytes);
    }
}
