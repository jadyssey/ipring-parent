package org.ipring.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author: lgj
 * @date: 2024/04/02 15:22
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum CommonStateEnum implements IntEnumType {

    UP(1, "上架"),
    DOWN(2, "下架"),
    ;
    private final Integer type;
    private final String description;



}
