package org.ipring.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author: Rainful
 * @date: 2024/04/09 14:16
 * @description:
 */
public abstract class CalcUtil {

    /**
     * 计算保留的最大精度
     */
    public static final int SCALE = 13;

    public static BigDecimal add(BigDecimal... args) {
        BigDecimal ret = BigDecimal.ZERO;
        for (BigDecimal arg : args) {
            ret = ret.add(arg);
        }
        return ret;
    }

    public static BigDecimal subtract(BigDecimal arg1, BigDecimal arg2) {
        return arg1.subtract(arg2);
    }

    public static BigDecimal subtract(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).subtract(BigDecimal.valueOf(arg2.doubleValue()));
    }

    public static BigDecimal multiply(Double arg1, Double arg2) {
        return BigDecimal.valueOf(arg1).multiply(BigDecimal.valueOf(arg2));
    }

    public static BigDecimal multiply(BigDecimal base, Number arg2) {
        return base.multiply(BigDecimal.valueOf(arg2.doubleValue()));
    }

    public static BigDecimal multiply(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).multiply(BigDecimal.valueOf(arg2.doubleValue()));
    }

    public static BigDecimal divide(Number arg1, Number arg2) {
        return BigDecimal.valueOf(arg1.doubleValue()).divide(BigDecimal.valueOf(arg2.doubleValue()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 除法
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static BigDecimal divide(BigDecimal arg1, BigDecimal arg2) {
        return arg1.divide(arg2, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 乘法
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static BigDecimal multiply(BigDecimal arg1, BigDecimal arg2) {
        return arg1.multiply(arg2);
    }

    public static BigDecimal pow(Number arg1, Number arg2) {
        return BigDecimal.valueOf(Math.pow(arg1.doubleValue(), arg2.doubleValue()));
    }
}
