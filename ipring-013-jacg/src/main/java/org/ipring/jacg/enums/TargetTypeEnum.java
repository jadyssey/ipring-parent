package org.ipring.jacg.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ipring.enums.StrEnumType;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: lgj
 * @date: 2024/04/02 14:26
 * @description:
 */
@RequiredArgsConstructor
@Getter
public enum TargetTypeEnum implements StrEnumType {

    URI("URI", "接口", ".*(/|controller).*"),
    MQ("MQ", "消息队列", ".*(rocket|mq).*"),
    JOB("Xxl-Job", "定时任务", ".*(job|Task#).*"),
    USELESS("useless?", "无用代码？", ".*(Service#|ServiceImpl#).*"),
    NO_Target("noTarget", "无终点", ""),
    OTHER("Other", "其他", ""),

    ;
    private final String type;
    private final String description;
    private final String regex;

    public static final Map<String, Pattern> ALL_ENUM_MAP =
            Arrays.stream(TargetTypeEnum.values()).collect(Collectors.toMap(TargetTypeEnum::getDescription, type -> Pattern.compile(type.regex, Pattern.CASE_INSENSITIVE)));

    public static String checkType(String input) {
        // 空值处理
        if (input == null || input.isEmpty()) {
            return USELESS.getDescription();
        }

        for (Map.Entry<String, Pattern> type : ALL_ENUM_MAP.entrySet()) {
            Matcher matcher = type.getValue().matcher(input);
            if (matcher.matches()) {
                return type.getKey();
            }
        }
        return OTHER.getDescription();
    }
}
