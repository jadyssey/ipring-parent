package org.ipring.pub;


import org.ipring.model.ZmqProperties;

/**
 * @author: lgj
 * @date: 2024/03/20 15:37
 * @description:
 */
public class ZmqPubTwo extends ZmqPubAbs {

    public ZmqPubTwo(ZmqProperties properties) {
        super(properties.getPublishTwo());
    }
}
