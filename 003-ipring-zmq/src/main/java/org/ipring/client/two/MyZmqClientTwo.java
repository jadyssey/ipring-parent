package org.ipring.client.two;

import org.springframework.stereotype.Component;

/**
 * @author lgj
 * @date 2024/5/14
 **/
@Component
public class MyZmqClientTwo extends MyZmqClient {
    public MyZmqClientTwo(MyZmqProperties myZmqProperties) {
        super(myZmqProperties.getTwo());
    }

    @Override
    public void dealWith(String data) {
        System.out.println(Thread.currentThread().getName() + " Two 收到消息 = " + data);
    }
}
