package org.ipring.util.ocr;

import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Component
public class DynamicSurveyValidator {
    // 从配置中心动态获取的日期参数（支持热更新）
    public static boolean isWithinSurveyPeriod() {
        Date nowDate = new Date();
        CronSequenceGenerator open = new CronSequenceGenerator("0 0 15 10 * *");
        Date openTime = open.next(nowDate);
        CronSequenceGenerator close = new CronSequenceGenerator("0 0 0 5 * *");
        Date closeTime = close.next(nowDate);

        // 从Cron表达式解析的时间参数（示例）
        final int OPEN_HOUR = openTime.getHours();    // open的小时字段
        final int OPEN_DAY = openTime.getDate();     // open的日字段

        final int CLOSE_HOUR = closeTime.getHours();   // close的小时字段
        final int CLOSE_DAY = closeTime.getDate();     // close的日字段


        LocalDateTime now = LocalDateTime.now();

        // 计算开始时间（当月或上月25日的10:30:00）
        LocalDateTime start;
        if (now.getDayOfMonth() >= OPEN_DAY) {
            start = LocalDateTime.of(now.getYear(), now.getMonth(), OPEN_DAY, OPEN_HOUR, 0);
        } else {
            LocalDateTime lastMonth = now.minusMonths(1);
            start = LocalDateTime.of(lastMonth.getYear(), lastMonth.getMonth(), OPEN_DAY, OPEN_HOUR, 0);
        }

        // 计算截止时间（下个月5日的22:45:00）
        LocalDateTime end = start.plusMonths(1).withDayOfMonth(CLOSE_DAY).withHour(CLOSE_HOUR);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public static void main(String[] args) {
        System.out.println(isWithinSurveyPeriod() ? "在收集期内" : "不在收集期内");
    }
}