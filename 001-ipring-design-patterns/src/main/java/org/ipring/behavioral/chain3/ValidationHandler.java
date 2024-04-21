package org.ipring.behavioral.chain3;


/**
 * @author lgj
 * @date 2024/4/17
 */
public interface ValidationHandler<T, E> {
    /**
     * 处理请求
     *
     * @param req
     * @param entry
     */
    void handleReq(T req, E entry);
}