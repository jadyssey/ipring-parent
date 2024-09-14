package org.ipring.structural.decorator;

import java.io.*;

/**
 * 具体组件提供操作的默认实现。这些类在程序中可能会有几个变体。
 *
 * @Author lgj
 * @Date 2024/4/24
 */
public class FileDataSource implements DataSource {

    private final String name;

    public FileDataSource(String name) {
        this.name = name;
    }

    @Override
    public void writeData(String data) {
        File file = new File(name);
        try (OutputStream fos = new FileOutputStream(file)) {
            fos.write(data.getBytes(), 0, data.length());
        } catch (IOException e) {
            System.out.println("写异常 = " + e.getMessage());
        }
        System.out.println("写入 data = " + data);
    }

    @Override
    public String readData() {
        char[] buffer = null;
        File file = new File(name);
        try (FileReader reader = new FileReader(file)) {
            buffer = new char[(int) file.length()];
            reader.read(buffer);
        } catch (IOException e) {
            System.out.printf("读异常 = ", e);
        }
        String data = new String(buffer);
        System.out.println("读取 data = " + data);
        return data;
    }
}
