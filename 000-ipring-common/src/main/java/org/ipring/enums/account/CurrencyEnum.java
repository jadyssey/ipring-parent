package org.ipring.enums.account;

import org.ipring.enums.StrEnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Rainful
 * @date: 2024/04/02 14:26
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum CurrencyEnum implements StrEnumType {

    USD("USD", "美元"),
    EUR("EUR", "欧元"),
    GBP("GBP", "英镑"),
    JPY("JPY", "日元"),
    CHF("CHF", "瑞士法郎"),
    CAD("CAD", "加拿大币"),
    AUD("AUD", "澳元"),
    NZD("NZD", "纽西兰元"),
    ;
    private final String type;
    private final String description;

    public static final List<CurrencyEnum> CURRENCY_ENUM_MAP = Arrays.asList(USD, EUR, GBP, JPY, CHF, CAD, AUD, NZD);
}
