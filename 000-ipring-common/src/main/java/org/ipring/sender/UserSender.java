package org.ipring.sender;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/13 11:08
 * @description:
 */
public interface UserSender<T> {

    /**
     * 广播
     */
    void send(T t);

    default void send(T t, List<CallBack> callBacks) {
        // todo
    }

    /**
     * 发送的时候会根据某个账号发
     */
    void send2Account(Long accountId, T t);

    default void send2Account(Long accountId, T t, List<CallBack> callBacks) {
        // todo
    }

    void send2UserId(Long userId, T t);

    default void send2UserId(Long userId, T t, List<CallBack> callBacks) {
        // todo
    }
}
