package org.ipring.structural.decorator;

/**
 * @Author lgj
 * @Date 2024/4/24
 */
public class DataSourceDecorator implements DataSource {
    // 装饰基类和其他组件遵循相同的接口。该类的主要任务是定义所有具体装饰的封
    // 装接口。封装的默认实现代码中可能会包含一个保存被封装组件的成员变量，并
    // 且负责对其进行初始化。
    protected DataSource wrappee;

    public DataSourceDecorator(DataSource wrappee) {
        this.wrappee = wrappee;
    }

    @Override
    public void writeData(String data) {
        this.wrappee.writeData(data);
    }

    @Override
    public String readData() {
        return this.wrappee.readData();
    }
}
