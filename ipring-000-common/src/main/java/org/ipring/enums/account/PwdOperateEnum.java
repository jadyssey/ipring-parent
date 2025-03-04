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
public enum PwdOperateEnum implements IntEnumType {

    UPDATE(1, "修改密码"),
    INIT(2, "重置"),
    ;

    private final Integer type;
    private final String description;
}
