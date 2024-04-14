package org.ipring.mq;

/**
 * @author: Rainful
 * @date: 2024/04/13 14:21
 * @description:
 */
public interface MqListener<MSG> {

    /**
     * 假设一个场景 Apod Bpod  A为发送端  B为接收端
     * 消息流转为 A -> B
     * 但是某一天需要 B自己也有一些业务 需要B发出去 B自己接收 那么就需要这个接口了 可以省去走mq的流程 改为调用本地方法自己消费了
     *
     * @param msg 消息
     */
    void message(MSG msg);
}
