package org.ipring.client.two;

import org.ipring.enums.order.OrderTypeEnum;
import org.ipring.httpclient.Ct4ServiceManager;
import org.ipring.model.SymbolMsgDTO;
import org.ipring.model.common.Return;
import org.ipring.model.httpclient.dto.OrderAddDTO;
import org.ipring.model.httpclient.response.ct4.ModifyOrderVO;
import org.ipring.util.CalcUtil;
import org.ipring.util.SymbolMsgUtil;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
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
        //ACC_TOKEN_MAP.put(618, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        //ACC_TOKEN_MAP.put(633, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        //ACC_TOKEN_MAP.put(659, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        /*ACC_TOKEN_MAP.put(446, "S3Js2MzilqRxmDEuHGsykupacesWbQac2/7gGRCmlI0=");
        ACC_TOKEN_MAP.put(448, "S3Js2MzilqRxmDEuHGsykum4henViWG6M+J/Pb0bsh8=");
        ACC_TOKEN_MAP.put(449, "S3Js2MzilqRxmDEuHGsykpstuJPh9f1keAjO1Sa2XSU=");
        ACC_TOKEN_MAP.put(453, "S3Js2MzilqRxmDEuHGsyklya/N3RvgjZ4E6a9b2+Bo0=");
        ACC_TOKEN_MAP.put(454, "S3Js2MzilqRxmDEuHGsyku2BftycBX03SkvEHD6jatM=");
        ACC_TOKEN_MAP.put(457, "S3Js2MzilqRxmDEuHGsykrcO+dePjs6unqNcibIcdiw=");
        ACC_TOKEN_MAP.put(458, "S3Js2MzilqRxmDEuHGsykvBSod2O9vdPB38TMbbazCY=");
        ACC_TOKEN_MAP.put(459, "S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc=");
        ACC_TOKEN_MAP.put(460, "S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc=");
        ACC_TOKEN_MAP.put(461, "S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc=");
        ACC_TOKEN_MAP.put(462, "S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc=");
        ACC_TOKEN_MAP.put(465, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(466, "4IQ9NtjlbuZu2btdSk4MyfySW5LIJQx9lnJJHV+3zds=");
        ACC_TOKEN_MAP.put(468, "udGkbiHACR4Ms+2daB9RKZdsFekZ/VE8qwE3qJC5Yv4=");
        ACC_TOKEN_MAP.put(469, "S3Js2MzilqRxmDEuHGsykn5uwFbpjPrVa4/nxry+/7g=");
        ACC_TOKEN_MAP.put(296, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        ACC_TOKEN_MAP.put(481, "S3Js2MzilqRxmDEuHGsykolfUG9y7mATW/wrTpRx3AM=");
        ACC_TOKEN_MAP.put(490, "S3Js2MzilqRxmDEuHGsykvpTkIrSx/KL2igATvKifUs=");
        ACC_TOKEN_MAP.put(491, "S3Js2MzilqRxmDEuHGsykiwga5a93LMstGGodvlA3vI=");
        ACC_TOKEN_MAP.put(532, "7izKDFHUjWMMYHULbxqsKyHtaRQ0jgc0hLDJVLQVAN8=");
        ACC_TOKEN_MAP.put(533, "7izKDFHUjWMMYHULbxqsKyHtaRQ0jgc0hLDJVLQVAN8=");
        ACC_TOKEN_MAP.put(534, "7izKDFHUjWMMYHULbxqsKyHtaRQ0jgc0hLDJVLQVAN8=");
        ACC_TOKEN_MAP.put(471, "S3Js2MzilqRxmDEuHGsykhMVVvtALeWJaYAsyPxx0qA=");
        ACC_TOKEN_MAP.put(253, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(288, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(447, "S3Js2MzilqRxmDEuHGsykvBSod2O9vdPB38TMbbazCY=");
        ACC_TOKEN_MAP.put(545, "S3Js2MzilqRxmDEuHGsykuf9BRlpMYp7ZmTI7Z3qC9Q=");
        ACC_TOKEN_MAP.put(544, "S3Js2MzilqRxmDEuHGsyks1AVxRfYmEVVm9dFnnfuAc=");
        ACC_TOKEN_MAP.put(328, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(323, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(371, "S3Js2MzilqRxmDEuHGsykmQCpiqGljl+bk1QjYus2Yo=");
        ACC_TOKEN_MAP.put(487, "S3Js2MzilqRxmDEuHGsykjTvZt19bI4sFoSYd5slmsI=");
        ACC_TOKEN_MAP.put(565, "S3Js2MzilqRxmDEuHGsykhMh7ti2JeGoVDkFYGlTCtQ=");
        ACC_TOKEN_MAP.put(549, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(562, "S3Js2MzilqRxmDEuHGsykpsPQopS6oDzmIGiyKbFj6M=");
        ACC_TOKEN_MAP.put(575, "S3Js2MzilqRxmDEuHGsykthzVrA22mmCCDcamu3Phl0=");
        ACC_TOKEN_MAP.put(577, "S3Js2MzilqRxmDEuHGsykthzVrA22mmCCDcamu3Phl0=");
        ACC_TOKEN_MAP.put(557, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(565, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(372, "S3Js2MzilqRxmDEuHGsykovuFWn71ywDQDvxgy67FlE=");
        ACC_TOKEN_MAP.put(591, "S3Js2MzilqRxmDEuHGsykgaSZUfvc0DuTf6yFiiGl44=");
        ACC_TOKEN_MAP.put(291, "S3Js2MzilqRxmDEuHGsykqV9C5iEhyLtJhqmnoAWWKc=");
        ACC_TOKEN_MAP.put(387, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(300, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(385, "S3Js2MzilqRxmDEuHGsykqdVUKd6l6fCcDlURscKhWk=");
        ACC_TOKEN_MAP.put(599, "S3Js2MzilqRxmDEuHGsykoIZTHWRJKY7P16P9OLAdGA=");
        ACC_TOKEN_MAP.put(547, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(297, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        ACC_TOKEN_MAP.put(610, "S3Js2MzilqRxmDEuHGsykn5uwFbpjPrVa4/nxry+/7g=");
        ACC_TOKEN_MAP.put(299, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(591, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(616, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(618, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        ACC_TOKEN_MAP.put(617, "S3Js2MzilqRxmDEuHGsykkMGlTUnrRQ86c9cvDOvErY=");
        ACC_TOKEN_MAP.put(582, "S3Js2MzilqRxmDEuHGsyklsEgnB/wrxkOTwGBNfphUc=");
        ACC_TOKEN_MAP.put(458, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(226, "S3Js2MzilqRxmDEuHGsyki2c8gxcGIqZ4vdherhPPmU=");
        ACC_TOKEN_MAP.put(633, "S3Js2MzilqRxmDEuHGsyknzI79yUb6QNOZRLuMIk2HA=");
        ACC_TOKEN_MAP.put(475, "S3Js2MzilqRxmDEuHGsykolfUG9y7mATW/wrTpRx3AM=");
        ACC_TOKEN_MAP.put(635, "S3Js2MzilqRxmDEuHGsykhkU6yqFTiLxhJPsqt00HzM=");
        ACC_TOKEN_MAP.put(637, "byJgMc2Oja2xppls2lIpTrSPjlboPxCCMAKMvlBYufA=");
        ACC_TOKEN_MAP.put(616, "S3Js2MzilqRxmDEuHGsykhIkNNpr0/5tVOy4HWV+Kok=");
        ACC_TOKEN_MAP.put(620, "S3Js2MzilqRxmDEuHGsykhIkNNpr0/5tVOy4HWV+Kok=");
        ACC_TOKEN_MAP.put(532, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(291, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(597, "S3Js2MzilqRxmDEuHGsykinQ2Uw5CMElacrhFVymZ6w=");
        ACC_TOKEN_MAP.put(641, "S3Js2MzilqRxmDEuHGsykistzuuK9tpNoibBt9tHMjg=");
        ACC_TOKEN_MAP.put(551, "S3Js2MzilqRxmDEuHGsykpsPQopS6oDzmIGiyKbFj6M=");
        ACC_TOKEN_MAP.put(651, "S3Js2MzilqRxmDEuHGsyktrLKdyS3k24dSSErauEUoo=");*/
        //ACC_TOKEN_MAP.put(288, "0b0f54a4c74a5909024afbeec3de20c4");
        //ACC_TOKEN_MAP.put(582, "0b0f54a4c74a5909024afbeec3de20c4");
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
            String[] msgArr = data.split(",");
            SymbolMsgDTO newMsgDto = SymbolMsgDTO.of(msgArr);
            OrderAddDTO req = new OrderAddDTO();
            List<Integer> acc = new ArrayList<>(ACC_TOKEN_MAP.keySet());
            if (acc.size() == 0) return;
            req.setAccountId(acc.get(ThreadLocalRandom.current().nextInt(acc.size())).longValue());
            req.setComment("压测单:" + LocalTime.now());
            req.setOperation(ThreadLocalRandom.current().nextInt(3)); // 市价单
            req.setSymbol(newMsgDto.getSymbol().getSymbolUniq());
            req.setTicket(0.01D);
            req.setExpirationType(ThreadLocalRandom.current().nextInt(2) == 0 ? 1 : 8);
            req.setExpiration(System.currentTimeMillis() + Duration.ofDays(ThreadLocalRandom.current().nextInt(30)).toMillis() + Duration.ofHours(48).toMillis());

            BigDecimal marketPrice = SymbolMsgUtil.openPrice(newMsgDto, req.getOperation());
            BigDecimal multiply = CalcUtil.multiply(ThreadLocalRandom.current().nextInt(2500, 6000), CalcUtil.pow(10, -newMsgDto.getPrecision()));
            if (OrderTypeEnum.BUY_LIMIT.getType().equals(req.getOperation()) || OrderTypeEnum.SELL_STOP.getType().equals(req.getOperation())) {
                // 需要在在市价下方指定位置
                BigDecimal temp = CalcUtil.subtract(marketPrice, multiply);
                req.setPrice(BigDecimal.ZERO.compareTo(temp) > 0 ? BigDecimal.ZERO : temp);
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
        LocalTime now = LocalTime.now();
        System.out.println("now = " + now);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000_000_000; i++) {
            int j = i + 1;
        }
        long end = System.currentTimeMillis();
        System.out.println("start - end = " + (end - start));
    }
}
