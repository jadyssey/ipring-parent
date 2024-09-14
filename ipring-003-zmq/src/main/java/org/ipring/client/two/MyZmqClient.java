package org.ipring.client.two;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.zeromq.*;

public abstract class MyZmqClient implements Runnable {
    private final String connectUri;
    private final Object lock = new Object(); // 使用一个显式的锁对象
    private volatile ZContext context;
    private volatile ZMQ.Socket socket;
    private volatile Thread runThread;
    private volatile Long latestHB;

    public MyZmqClient(String connectUri) {
        if (StringUtils.isBlank(connectUri)) {
            throw new IllegalArgumentException("Invalid connectUri");
        }
        this.connectUri = connectUri;
        initZMQ();
    }

    /**
     * 处理接收到数据的抽象方法
     */
    public abstract void dealWith(String data);

    @Scheduled(fixedRate = 10 * 1000)
    public void checkAlive() {
        if (latestHB != null && System.currentTimeMillis() - latestHB > 60 * 1000) {
            System.out.println("重连中..   connectUri = " + connectUri + "  thread=" + Thread.currentThread().getName());
            reconnect();
        }
    }

    @Override
    public void run() {
        ZThread.start(args -> executeZeroMQThread());
    }

    private void executeZeroMQThread() {
        System.out.println("zero mq 开始运行 thread=" + Thread.currentThread().getName());

        synchronized (lock) {
            if (runThread != null && runThread != Thread.currentThread()) {
                runThread.interrupt();
            }
            runThread = Thread.currentThread();
        }

        try {
            while (!Thread.currentThread().isInterrupted()) {
                processReceivedMessages();
            }
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (runThread != null && runThread == Thread.currentThread()) {
                synchronized (lock) {
                    if (runThread != null && runThread == Thread.currentThread()) runThread = null; // 确保在结束时将 runThread 置为 null
                }
            }
        }
    }

    private void processReceivedMessages() {
        String recvStr = socket.recvStr();
        if (recvStr == null) return;
        latestHB = System.currentTimeMillis();
        this.dealWith(recvStr);
    }

    private void handleException(Exception e) {
        System.err.println("当前线程异常，interrupt thread =" + Thread.currentThread().getName());
        e.printStackTrace(); // 输出异常堆栈信息
        // 可以在此处添加额外的异常处理逻辑，如日志记录等
    }

    private void initZMQ() {
        context = new ZContext();
        socket = context.createSocket(SocketType.SUB);
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
        try {
            socket.connect(connectUri);
        } catch (ZMQException e) {
            handleException(e);
            close(); // 连接失败时关闭资源
        }
    }

    private void reconnect() {
        this.close();
        this.initZMQ();
        this.run();
    }

    private void close() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
        latestHB = 0L;
    }
}