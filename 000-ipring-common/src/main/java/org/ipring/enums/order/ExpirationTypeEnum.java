package org.ipring.enums.order;

import org.ipring.enums.IntEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author lgj
 * @date 2024/4/11
 **/
@Getter
@AllArgsConstructor
public enum ExpirationTypeEnum implements IntEnumType {
    /*
    挂单过期时间设置模式
    过期时间必须大于等于当前时间 60 秒钟（CT4 以前定义为 10 分钟），
    且如果指定的时间点处于收市期间，那么在本次收市完后下次一开盘就立马删掉此挂单。
     */
    GOOD_TILL_CANCELED(1, "长期有效（手动取消）"),
    INTRADAY(2, "当日有效（今日收盘时自动取消）"),
    SPECIFIED_DAY(4, "指定日有效（指定具体的取消日期，在该日收盘时自动取消）"),
    SPECIFIED(8, "指定时间有效（指定具体的取消时间）"),
    ;

    private final Integer type;
    private final String description;
}
