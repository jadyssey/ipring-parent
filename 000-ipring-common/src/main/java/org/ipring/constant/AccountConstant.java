package org.ipring.constant;

import java.math.BigDecimal;

/**
 * @author: Rainful
 * @date: 2024/04/02 14:56
 * @description:
 */
public interface AccountConstant {

    Long accountStoreKey = -1L;

    int maxDemoAcc = 20;

    int demoAccount = 2;
    BigDecimal fbMarginCall = BigDecimal.ONE;
    BigDecimal fbStopOutLevel = BigDecimal.valueOf(0.25);
}
