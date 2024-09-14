package org.ipring.client.one;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.ipring.model.ClientInfo;
import org.ipring.model.ZmqConstant;
import org.ipring.sender.NoticeService;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.net.Inet4Address;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author: lgj
 * @date: 2024/03/19 12:25
 * @description:
 */
@Slf4j
public abstract class ZmqClientAbs implements ZmqClient {

    private final Environment env;
    private final String subAddress;
    private boolean canRun;
    private LocalDateTime latestHB;
    private final NoticeService<String> noticeService;
    private final static ScheduledExecutorService HEART_CHECK_EXECUTORS = Executors.newScheduledThreadPool(6, new DefaultThreadFactory("heart-check"));

    public ZmqClientAbs(Environment env, String subAddress, NoticeService<String> noticeService) {
        this.env = env;
        this.subAddress = subAddress;
        this.noticeService = noticeService;
        this.canRun = true;
        this.latestHB = LocalDateTime.now();
    }

    @Override
    public void run() {
        checkAlive();
        subscribe();
    }

    protected void subscribe() {
        final Optional<ClientInfo> infoOp = Optional.ofNullable(this.getClass().getDeclaredAnnotation(ClientInfo.class));
        infoOp.ifPresent(ele -> log.info("{}客户端启动, 监听地址:{}", ele.marketName(), subAddress));

        try (final ZContext context = new ZContext();
             final ZMQ.Socket socket = context.createSocket(SocketType.SUB)) {
            socket.setLinger(0);
            socket.connect(subAddress);
            subscribeTopic(socket); // 不知道为什么其他都是全部 但是A股和港股写的 sub.subscribe(TickMobileConst.HEARTBEAT); sub.subscribe(SUB_PREFIX);

            while (canRun && !Thread.currentThread().isInterrupted()) {
                final String recvStr = socket.recvStr();
                latestHB = LocalDateTime.now(); // 外汇的心跳没有了... 不知道为什么 不放在这里 就会导致心跳一直不更新 就酱紫吧
                if (recvStr.startsWith(heartBeatStr())) {
                    continue;
                }
                // tickHandler.handler(recvStr); // 准备抽出handler接口的 但是但是还是先不动这块儿了
                handlerTick(recvStr);
            }

            socket.disconnect(subAddress);
            context.destroySocket(socket);
        }
    }

    protected abstract void handlerTick(String recvStr);

    protected String heartBeatStr() {
        return ZmqConstant.HEART_BEAT;
    }

    protected void subscribeTopic(ZMQ.Socket socket) {
        socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
    }

    @Scheduled(cron = "0 * * * * ?")
    protected void checkAlive() {
        if (Duration.between(latestHB, LocalDateTime.now()).toMinutes() > 1) reconnect();
    }

    protected void reconnect() {
        try {
            latestHB = LocalDateTime.now();
            final Optional<ClientInfo> infoOp = Optional.ofNullable(this.getClass().getDeclaredAnnotation(ClientInfo.class));
            final String name = infoOp.map(ClientInfo::marketName).orElse("未设置");
            log.info("【tick-mobile-{}】上游{}订阅源长时间无心跳, 重连中...zmq-sub地址【{}】", env.getActiveProfiles()[0], name, subAddress);

            canRun = false;
            Thread.sleep(5000L);
            canRun = true;
            noticeService.notice(String.format("【%s】【tick-mobile】【%s】ZMQ地址【%s】心跳检测到异常，pod=%s最近一次心跳时间%s，当前时间%s",
                    env.getActiveProfiles()[0], name, Inet4Address.getLocalHost().getHostAddress(), Inet4Address.getLocalHost().getHostAddress(), latestHB, LocalDateTime.now()));
            subscribe();
            log.info("{}重连完成", name);
        } catch (Exception e) {
            log.error("zmq订阅上游重连异常", e);
        }
    }
}
