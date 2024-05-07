package org.ipring.client.sender;

/**
 * @author: lgj
 * @date: 2024/04/02 19:57
 * @description:
 */
public interface TickSender<T> {

    void send(T data);
}
