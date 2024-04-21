package org.ipring.zmq.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.sender.TickSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocalTickSaver implements TickSender<String> {

    private ConcurrentHashMap<Long, Long> COUNT_MAP = new ConcurrentHashMap<>();
    private ConcurrentSkipListSet<String> COUNT_SYMBOL = new ConcurrentSkipListSet<>();
    private ConcurrentHashMap<String, LongAdder> GROUP_SYMBOL = new ConcurrentHashMap<>();

    private final ThreadPoolTaskExecutor commonThreadPool;

    private final LongAdder longAdder = new LongAdder();

    @Override
    public void send(String data) {
        long now = System.currentTimeMillis() / 1000;
        longAdder.increment();
        commonThreadPool.execute(() -> {
            COUNT_MAP.put(now, longAdder.sum());

            String symbolUniq = data.split(",")[0];
            COUNT_SYMBOL.add(symbolUniq);
            LongAdder longAdder = GROUP_SYMBOL.computeIfAbsent(symbolUniq, key -> new LongAdder());
            longAdder.increment();
            //log.info("data = {}", symbolUniq);
        });
        //String symbolUniq = data.split(",")[0];
        //log.info("data = {}", symbolUniq);
    }

    @Scheduled(fixedRate = 1000)
    public void test() {
        long now = System.currentTimeMillis() / 1000;
        Long prev = COUNT_MAP.get(now - 1);
        Long prev2 = COUNT_MAP.get(now - 2);
        int size = COUNT_SYMBOL.size();
        COUNT_SYMBOL.clear();
        Set<Map.Entry<String, LongAdder>> entries = GROUP_SYMBOL.entrySet();
        List<Long> list = entries.stream().map(entry -> entry.getValue().sum()).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        long sum = list.stream().mapToLong(Long::longValue).sum();
        GROUP_SYMBOL.clear();
        log.info("QPS 报价次数={}, 总报价量={}, 品种数量={}, 品种次数排行 = {}", prev - prev2, prev, size + "_" + sum, list);
    }
}