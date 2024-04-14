package org.ipring.enums.account;

import org.ipring.enums.IntEnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author: Rainful
 * @date: 2024/04/02 19:50
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum AccountRoleEnum implements IntEnumType {

    TRADER(1, "可交易"),
    VIEWER(2, "观摩"),
    ;
    private final Integer type;
    private final String description;
}
