package org.ipring.enums.common;

import org.ipring.enums.StrEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author lgj
 * @date 9/2/2023
 **/
@Getter
@AllArgsConstructor
public enum ServiceSubCodeStr implements StrEnumType {

    // 其他
    SYSTEM("00", "00~09为系统保留code类型"),

    ORDER("11", "order"),

    VOTE_FRONTEND("12", "controller2"),

    ACCOUNT("20", "账户相关"),
    ;

    private final String type;

    private final String description;
}
