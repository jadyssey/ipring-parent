package org.ipring.client.two;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

public abstract class MyZmqClient2 implements Runnable {
    private final String connectUri;

    private volatile ZContext context = null;
    private volatile ZMQ.Socket socket = null;
    private volatile Thread runThread;
    private volatile Long latestHB;

    public MyZmqClient2(String connectUri) {
        if (!StringUtils.hasText(connectUri)) {
            throw new IllegalArgumentException("Invalid connectUri");
        }
        this.connectUri = connectUri;
        initZMQ();
    }

    /**
     * 处理接收到数据的抽象方法
     */
    public abstract void dealWith(String data);

    @Override
    public void run() {
        ZThread.start(this::executeZeroMQThread);
    }

    private void executeZeroMQThread(Object arg) {
        System.out.println("zero mq 开始运行 thread=" + Thread.currentThread().getName());

        synchronized (this) {
            if (runThread != null && runThread != Thread.currentThread()) {
                runThread.interrupt();
            }
            runThread = Thread.currentThread();
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                processReceivedMessages();
            } catch (Exception e) {
                handleException(e);
                break; // 假设遇到异常后需要退出循环
            }
        }
    }

    private void processReceivedMessages() throws Exception {
        String recvBuf = socket.recvStr();
        if (recvBuf == null) return;
        latestHB = System.currentTimeMillis();
        this.dealWith(recvBuf);
    }


    private void handleException(Exception e) {
        System.out.println("当前线程异常，interrupt thread =" + Thread.currentThread().getName());
        Thread.currentThread().interrupt();
        // 可以在此处添加额外的异常处理逻辑，如日志记录等
    }


    @Scheduled(fixedRate = 5000)
    protected void checkAlive() {
        // 超过一分钟无消息自动重连
        if (latestHB != null && System.currentTimeMillis() - latestHB > 60 * 1000) {
            System.out.println("重连中..   connectUri = " + connectUri + "  thread=" + Thread.currentThread().getName());
            reconnect();
        }
    }

    private void initZMQ() {
        context = new ZContext();
        socket = context.createSocket(SocketType.SUB);
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
        socket.connect(connectUri);
    }

    private void reconnect() {
        this.close();
        this.initZMQ();
        this.run();
    }

    private void close() {
        if (socket != null) {
            socket.close();
        }
        if (context != null) {
            context.close();
        }
    }
}