package org.ipring.model.entity.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author yb
 * @date: 2024/4/11
 */
@Getter
@Setter
public class TimeStartEnd {
    private int open;
    private int close;

    public static boolean isInTime(List<TimeStartEnd> list, int now) {
        for (TimeStartEnd timeStartEnd : list) {
            if (now >= timeStartEnd.getOpen() && now < timeStartEnd.getClose()) return true;
        }
        return false;
    }
}
