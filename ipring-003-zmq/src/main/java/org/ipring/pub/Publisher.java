package org.ipring.pub;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// todo 使用synchronized性能更好一点，不过要防止死锁的情况？
public class Publisher {

    private final Lock lock = new ReentrantLock();
    private ZMQ.Socket publisher;
    private ZContext context;
    private final String endpoint;

    public Publisher(String endpoint) {
        this.endpoint = endpoint;
        reconnect();
        System.out.println("Publisher started");
    }

    private void reconnect() {
        lock.lock(); // 获取锁
        try {
            if (publisher != null) {
                publisher.close();
            }
            if (context != null) {
                context.close();
            }
            // 创建 ZeroMQ 上下文
            context = new ZContext();
            // 创建发布者套接字
            publisher = context.createSocket(SocketType.PUB);
            // 绑定到指定端口
            publisher.bind(endpoint);
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    public void send(String message) {
        lock.lock(); // 获取锁
        try {
            try {
                // 发布消息
                publisher.send(message.getBytes(), 0);
            } catch (Exception e) {
                System.out.println("Exception occurred: " + e.getMessage());
                // 出现异常时重新连接
                reconnect();
                // 发送消息
                publisher.send(message.getBytes(), 0);
                System.out.println("Published (after reconnect): " + message);
            }
        } finally {
            lock.unlock(); // 释放锁
        }
    }

    public void close() {
        lock.lock(); // 获取锁
        try {
            // 关闭套接字和上下文
            publisher.close();
            context.close();
            System.out.println("Publisher closed");
        } finally {
            lock.unlock(); // 释放锁
        }
    }
}
