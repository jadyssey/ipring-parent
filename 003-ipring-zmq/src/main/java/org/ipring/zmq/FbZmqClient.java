package org.ipring.zmq;

import org.ipring.sender.NoticeService;
import org.ipring.sender.TickSender;
import org.ipring.zmq.model.ClientInfo;
import org.ipring.zmq.model.ZmqConstant;
import org.ipring.zmq.model.ZmqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/03 17:13
 * @description:
 */
@Component
@ConditionalOnProperty("zmq.subscribe-address.fb")
@ClientInfo(marketName = ZmqConstant.FB)
public class FbZmqClient extends ZmqClientAbs {

    private final List<TickSender<String>> tickSenders;


    public FbZmqClient(Environment env, ZmqProperties properties, NoticeService<String> noticeService, List<TickSender<String>> tickSenders) {
        super(env, properties.getSubscribeAddress().getFb(), noticeService);
        this.tickSenders = tickSenders;
    }

    private static volatile long lastTime = 0;

    @Override
    protected void handlerTick(String recvStr) {
        long now = System.currentTimeMillis();
        System.out.println("报价间隔 = " + (now - lastTime));
        lastTime = now;
        //tickSenders.forEach(sender -> sender.send(recvStr));
    }
}
