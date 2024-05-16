package org.ipring.client.two;

import org.ipring.client.Ct4ServiceManager;
import org.ipring.client.dto.OrderAddDTO;
import org.ipring.client.response.ct4.ModifyOrderVO;
import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.model.SymbolMsgDTO;
import org.ipring.model.common.Return;
import org.ipring.util.CalcUtil;
import org.ipring.util.SymbolMsgUtil;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lgj
 * @date 2024/5/14
 **/
@Component
public class MyZmqClientTwo extends MyZmqClient {

    private static final Map<Integer, String> ACC_TOKEN_MAP = new HashMap<>();

    static {
        //ACC_TOKEN_MAP.put(296, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        //ACC_TOKEN_MAP.put(297, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        ACC_TOKEN_MAP.put(288, "0b0f54a4c74a5909024afbeec3de20c4");
        ACC_TOKEN_MAP.put(582, "0b0f54a4c74a5909024afbeec3de20c4");
    }

    private final Ct4ServiceManager ct4ServiceManager;
    private final ThreadPoolTaskExecutor commonThreadPool;
    private final AtomicLong atomicLong = new AtomicLong();

    public MyZmqClientTwo(MyZmqProperties myZmqProperties, Ct4ServiceManager ct4ServiceManager, ThreadPoolTaskExecutor commonThreadPool) {
        super(myZmqProperties.getTwo());
        this.ct4ServiceManager = ct4ServiceManager;
        this.commonThreadPool = commonThreadPool;
    }

    @Override
    public void dealWith(String data) {
        commonThreadPool.execute(() -> {
            long val = atomicLong.get();
            if (val > 1) return; // todo

            String[] msgArr = data.split(",");
            SymbolMsgDTO newMsgDto = SymbolMsgDTO.of(msgArr);
            OrderAddDTO req = new OrderAddDTO();
            List<Integer> acc = new ArrayList<>(ACC_TOKEN_MAP.keySet());
            req.setAccountId(acc.get(ThreadLocalRandom.current().nextInt(acc.size())).longValue());
            req.setComment("压测下单: " + LocalDateTime.now());
            req.setOperation(ThreadLocalRandom.current().nextInt(6));
            req.setSymbol(newMsgDto.getSymbol().getSymbolUniq());
            req.setTicket(0.01D);
            req.setExpirationType(ThreadLocalRandom.current().nextInt(2) == 0 ? 1 : 8);
            req.setExpiration(System.currentTimeMillis() + Duration.ofHours(ThreadLocalRandom.current().nextInt(48)).toMillis() + Duration.ofHours(1).toMillis());

            BigDecimal marketPrice = SymbolMsgUtil.closePrice(newMsgDto, req.getOperation());
            BigDecimal multiply = CalcUtil.multiply(ThreadLocalRandom.current().nextInt(500, 2000), CalcUtil.pow(10, -newMsgDto.getPrecision()));
            if (OrderTypeEnum.BUY_LIMIT.getType().equals(req.getOperation()) || OrderTypeEnum.SELL_STOP.getType().equals(req.getOperation())) {
                // 需要在在市价下方指定位置
                req.setPrice(CalcUtil.subtract(marketPrice, multiply));
            } else if (OrderTypeEnum.SELL_LIMIT.getType().equals(req.getOperation()) || OrderTypeEnum.BUY_STOP.getType().equals(req.getOperation())) {
                // 需要在在市价上方指定位置
                req.setPrice(CalcUtil.add(marketPrice, multiply));
            } else {
                req.setPrice(BigDecimal.ZERO);
            }
            Return<ModifyOrderVO> res = ct4ServiceManager.makeOrder(ACC_TOKEN_MAP.get(req.getAccountId().intValue()), req);
            if (res.success()) {
                atomicLong.incrementAndGet();
            }
        });
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            BigDecimal multiply = CalcUtil.multiply(ThreadLocalRandom.current().nextInt(10, 100), CalcUtil.pow(10, -4));
            System.out.println("v = " + multiply.toPlainString());
        }
    }
}
