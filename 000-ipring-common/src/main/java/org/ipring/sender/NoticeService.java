package org.ipring.sender;

/**
 * @author: lgj
 * @date: 2024/04/03 15:33
 * @description:
 */
public interface NoticeService<T> {

    void notice(T msg);
}
