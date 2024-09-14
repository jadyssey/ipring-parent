package org.ipring.time;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LatestTimeExample {
    public static void main(String[] args) {
        // 获取当天最大时间
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        
        // 显示结果
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println("当前日期的最晚时间: " + endOfDay.format(formatter));
        
        // 获取当前时间的最大时间戳
        ZonedDateTime endOfDayZoned = endOfDay.atZone(ZoneId.systemDefault());
        System.out.println("当前日期的最晚时间戳: " + endOfDayZoned.toInstant().toEpochMilli());

        LocalTime max = LocalTime.MAX;
    }
}
