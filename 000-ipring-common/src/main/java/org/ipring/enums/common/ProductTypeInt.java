package org.ipring.enums.common;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lgj
 * @date 8/2/2023
 */
@Getter
@AllArgsConstructor
public enum ProductTypeInt implements IntEnumType {
    // 其他
    OTHER(0, "其他"),

    FXCHAT(1, "汇聊"),

    FX110(2, "FX110"),

    FORUM(3, "论坛"),

    BL_FINANCE(4, "拜仑财经"),

    NIU(5, "牛人榜"),

    EVALUATE(6, "云评测"),

    GOLD_MALL(7, "金币商城"),

    GOLD_RED_PACKET(8, "金币红包"),

    FX_KNOWN(9, "汇乎"),

    FX_CHACHA(10, "汇查查"),

    FX_HEADLINE(11, "外汇头条"),

    HUIKETANG(13, "汇课堂"),

    PRU(14, "普鲁社"),

    FAZZACO(15, "fazzaco"),

    HUIHUN(16, "汇魂"),

    TW_FX110(17, "台湾版FX110"),

    CT4(22, "CT4"),

    FASTBULL(26, "FastBull"),

    BROKERS_VIEW(27, "BrokersView"),

    FABUCAIJING(37, "法布财经"),

    FASTBULL_LIVE(39, "FastBull.live");

    private final Integer type;
    private final String description;


    private static final Map<Integer, ProductTypeInt> ALL_PRODUCT_TYPE_MAP = new HashMap<>();

    static {
        for (ProductTypeInt client : ProductTypeInt.values()) {
            ALL_PRODUCT_TYPE_MAP.put(client.getType(), client);
        }
    }

    public static ProductTypeInt getProductType(int type) {
        return ALL_PRODUCT_TYPE_MAP.get(type);
    }
}
