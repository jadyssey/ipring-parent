package org.ipring.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * @author: Rainful
 * @date: 2024/04/09 14:16
 * @description:
 */
@Slf4j
public abstract class CalcUtil {

    /**
     * 计算保留的最大精度
     */
    public static final int SCALE = 13;

    public static BigDecimal add(BigDecimal... args) {
        BigDecimal ret = BigDecimal.ZERO;
        for (BigDecimal arg : args) {
            if (Objects.isNull(arg)) continue;
            ret = ret.add(arg);
        }
        return ret;
    }

    public static BigDecimal subtract(BigDecimal arg1, BigDecimal arg2) {
        return arg1.subtract(arg2).stripTrailingZeros();
    }

    public static BigDecimal subtract(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).subtract(BigDecimal.valueOf(arg2.doubleValue())).stripTrailingZeros();
    }

    public static BigDecimal multiply(Double arg1, Double arg2) {
        return BigDecimal.valueOf(arg1).multiply(BigDecimal.valueOf(arg2)).stripTrailingZeros();
    }

    public static BigDecimal multiply(BigDecimal base, Number arg2) {
        return base.multiply(BigDecimal.valueOf(arg2.doubleValue())).stripTrailingZeros();
    }

    public static BigDecimal multiply(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).multiply(BigDecimal.valueOf(arg2.doubleValue())).stripTrailingZeros();
    }

    public static BigDecimal multiply(Number... args) {
        BigDecimal ret = BigDecimal.ZERO;
        for (Number arg : args) {
            if (Objects.isNull(arg)) continue;
            ret = ret.multiply(BigDecimal.valueOf(arg.doubleValue()));
        }
        return ret;
    }

    public static BigDecimal divide(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).divide(BigDecimal.valueOf(arg2.doubleValue()), SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /**
     * 除法
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static BigDecimal divide(BigDecimal arg1, BigDecimal arg2) {
        return arg1.divide(arg2, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /**
     * 乘法
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static BigDecimal multiply(BigDecimal arg1, BigDecimal arg2) {
        return arg1.multiply(arg2).stripTrailingZeros();
    }

    public static BigDecimal pow(Number arg1, Number arg2) {
        return BigDecimal.valueOf(Math.pow(arg1.doubleValue(), arg2.doubleValue())).stripTrailingZeros();
    }

    public static String formatBd(BigDecimal origin) {
        return origin.stripTrailingZeros().toPlainString();
    }
}
