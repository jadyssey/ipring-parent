package org.ipring.client.two;

/**
 * @author lgj
 * @date 2024/5/14
 **/
public class MyZmqClientOne extends MyZmqClient {

    public MyZmqClientOne(MyZmqProperties myZmqProperties) {
        super(myZmqProperties.getSubOne());
    }

    @Override
    public void dealWith(String data) {
        System.out.println(Thread.currentThread().getName() + " One 收到消息 = " + data);
    }
}
