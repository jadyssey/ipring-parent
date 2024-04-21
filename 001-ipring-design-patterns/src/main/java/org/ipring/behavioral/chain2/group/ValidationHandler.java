package org.ipring.behavioral.chain2.group;

public interface ValidationHandler<T> {
    void handleReq(T req);
}