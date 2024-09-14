package org.ipring.pub;


import org.ipring.model.ZmqProperties;

/**
 * @author: lgj
 * @date: 2024/03/20 15:37
 * @description:
 */
public class ZmqPubOne extends ZmqPubAbs {

    public ZmqPubOne(ZmqProperties properties) {
        super(properties.getPublishOne());
    }
}
