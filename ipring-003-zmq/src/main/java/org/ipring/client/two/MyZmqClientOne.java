package org.ipring.client.two;

import lombok.extern.slf4j.Slf4j;
import org.ipring.model.SymbolMsgDTO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * @author lgj
 * @date 2024/5/14
 **/
@Slf4j
@Component
public class MyZmqClientOne extends MyZmqClient {
    private final ConcurrentHashMap<Long, Long> COUNT_MAP = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<String> COUNT_SYMBOL = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<String, LongAdder> GROUP_SYMBOL = new ConcurrentHashMap<>();
    private final AtomicLong atomicLong = new AtomicLong();

    @Resource
    private ThreadPoolTaskExecutor commonThreadPool;

    public MyZmqClientOne(MyZmqProperties myZmqProperties) {
        super(myZmqProperties.getOne());
    }

    @Override
    public void dealWith(String data) {
        if (true) return;
        if (data.startsWith("8")) {
            SymbolMsgDTO newMsgDto = SymbolMsgDTO.of(data.split(","));
            if (newMsgDto == null) return;
            Instant instant = Instant.ofEpochSecond(newMsgDto.getTime());
            // 将 Instant 转换为 LocalDateTime（默认时区）
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            log.info("8100_EURUSD 报价 {}, data = {}", localDateTime, data);
        }
        long now = System.currentTimeMillis() / 1000;
        long curr = atomicLong.incrementAndGet();
        commonThreadPool.execute(() -> {
            COUNT_MAP.put(now, curr);

            String symbolUniq = data.split(",")[0];
            COUNT_SYMBOL.add(symbolUniq);
            LongAdder longAdder = GROUP_SYMBOL.computeIfAbsent(symbolUniq, key -> new LongAdder());
            longAdder.increment();
            //log.info("data = {}", symbolUniq);
        });
    }

    @Scheduled(fixedRate = 1000)
    public void test() {
        long now = System.currentTimeMillis() / 1000;
        Long prev = COUNT_MAP.get(now - 1);
        Long prev2 = COUNT_MAP.get(now - 2);
        if (Objects.isNull(prev2)) return;

        int size = COUNT_SYMBOL.size();
        COUNT_SYMBOL.clear();
        Set<Map.Entry<String, LongAdder>> entries = GROUP_SYMBOL.entrySet();
        List<Long> list = entries.stream().map(entry -> entry.getValue().sum()).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        long sum = list.stream().mapToLong(Long::longValue).sum();
        GROUP_SYMBOL.clear();
        log.info("QPS 报价次数={}, 总报价量={}, 品种数量={}, 品种次数排行 = {}", prev - prev2, prev, size + "_" + sum, list);
    }
}
