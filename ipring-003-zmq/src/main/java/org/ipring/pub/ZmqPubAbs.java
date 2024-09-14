package org.ipring.pub;

import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import javax.annotation.PreDestroy;

/**
 * @author: lgj
 * @date: 2024/03/20 15:27
 * @description:
 */
@Slf4j
public abstract class ZmqPubAbs {

    private final String bindAddr;
    private final ZContext zContext;
    private volatile ZMQ.Socket socket;

    public ZmqPubAbs(String bindAddr) {
        this.bindAddr = bindAddr;
        zContext = new ZContext();
        socket = zContext.createSocket(SocketType.PUB);
        socket.bind(bindAddr);
        log.info("端口地址:{} ZMQ启动", bindAddr);
    }

    public synchronized void send(String msg) {
        socket.send(msg);
    }

    public synchronized void reconnect() {
        ZMQ.Socket newSocket = zContext.createSocket(SocketType.PUB);
        ZMQ.Socket oldSocket = this.socket;
        this.socket = newSocket;
        zContext.destroySocket(oldSocket);
        newSocket.bind(bindAddr);
    }

    @PreDestroy
    public void terminate() {
        zContext.close();
    }
}
