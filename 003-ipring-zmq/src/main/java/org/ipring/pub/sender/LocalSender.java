package org.ipring.pub.sender;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipring.pub.ZmqPubAbs;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class LocalSender implements Sender {

    private final List<ZmqPubAbs> pubs;

    @Override
    @SneakyThrows
    public void send(String data) {
        log.debug("时间:{}, msg:{}", LocalDateTime.now(), data);
        for (ZmqPubAbs pub : pubs) {
            pub.send(data);
        }
    }
}