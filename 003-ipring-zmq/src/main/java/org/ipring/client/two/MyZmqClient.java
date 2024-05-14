package org.ipring.client.two;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;


public abstract class MyZmqClient implements Runnable {
    private final String connectUri;

    private volatile ZContext context = null;
    private volatile ZMQ.Socket socket = null;
    private volatile Thread runThread;
    private volatile Long latestHB;

    public MyZmqClient(String connectUri) {
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
        ZThread.start(arg -> {
            System.out.println("zero mq 开始运行 thread=" + Thread.currentThread().getName());

            if (runThread != null && runThread != Thread.currentThread()) runThread.interrupt();
            runThread = Thread.currentThread();

            while (!Thread.currentThread().isInterrupted() && runThread == Thread.currentThread()) {
                try {
                    String recvBuf = socket.recvStr();
                    latestHB = System.currentTimeMillis();
                    if (recvBuf == null) continue;

                    this.dealWith(recvBuf);
                } catch (Exception e) {
                    System.out.println("当前线程异常，interrupt thread =" + Thread.currentThread().getName());
                    Thread.currentThread().interrupt();
                }
            }
        });
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