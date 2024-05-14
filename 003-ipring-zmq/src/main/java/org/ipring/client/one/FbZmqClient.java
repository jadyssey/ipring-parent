package org.ipring.client.one;

import org.ipring.client.one.sender.TickSender;
import org.ipring.sender.NoticeService;
import org.ipring.model.ClientInfo;
import org.ipring.model.ZmqConstant;
import org.ipring.model.ZmqProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: lgj
 * @date: 2024/04/03 17:13
 * @description:
 */
@Component
@ConditionalOnProperty("zmq.subscribe-address.fb")
@ClientInfo(marketName = ZmqConstant.FB)
public class FbZmqClient extends ZmqClientAbs {

    private final List<TickSender<String>> tickSenders;


    public FbZmqClient(Environment env, ZmqProperties properties, NoticeService<String> noticeService, List<TickSender<String>> tickSenders) {
        super(env, properties.getSubscribeAddress().getLocalOne(), noticeService);
        this.tickSenders = tickSenders;
    }

    private static final long lastTime = 0;

    @Override
    protected void handlerTick(String recvStr) {
        System.out.println("recvStr = " + recvStr);
        //long now = System.currentTimeMillis();
        //System.out.println("报价间隔 = " + (now - lastTime));
        //lastTime = now;
        tickSenders.forEach(sender -> sender.send(recvStr));
    }
}
