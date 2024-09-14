package org.ipring.task;

import org.springframework.stereotype.Component;

@Component
public class MyBean {

    public void hello(String hname){
        System.out.println("===myBean执行====");
        System.out.println("你好："+hname);//打印   你好：中国
    }

}