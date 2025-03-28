package org.ipring.enums.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ipring.enums.IntEnumType;

/**
 * @author: lgj
 * @date: 2024/04/02 19:24
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum PwdTypeEnum implements IntEnumType {

    TRADING_PWD(1, "交易密码"),
    VIEW_PWD(2, "观摩密码"),
    ;

    private final Integer type;
    private final String description;
}
