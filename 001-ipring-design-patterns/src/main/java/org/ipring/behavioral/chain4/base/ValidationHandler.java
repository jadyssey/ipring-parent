package org.ipring.behavioral.chain4.base;


import org.ipring.enums.SubCode;

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
    SubCode handleReq(T req, E entry);
}