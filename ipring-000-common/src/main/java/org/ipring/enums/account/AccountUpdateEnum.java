package org.ipring.enums.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ipring.enums.IntEnumType;

/**
 * @author: lgj
 * @date: 2024/04/13 14:30
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum AccountUpdateEnum implements IntEnumType {

    CREATE(1, "创建账户"),
    LOGIN(2, "登录账户"),
    LOGIN_OUT(3, "登出账户"),
    ;
    private final Integer type;
    private final String description;
}
