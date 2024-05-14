package org.ipring.client.two;

/**
 * @author lgj
 * @date 2024/5/14
 **/
public class MyZmqClientTwo extends MyZmqClient {
    public MyZmqClientTwo(MyZmqProperties myZmqProperties) {
        super(myZmqProperties.getSubTwo());
    }

    @Override
    public void dealWith(String data) {
        System.out.println(Thread.currentThread().getName() + " Two 收到消息 = " + data);
    }
}
