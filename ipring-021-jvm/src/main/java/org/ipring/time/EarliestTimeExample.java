package org.ipring.time;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EarliestTimeExample {

    public static void main(String[] args) {
        // 获取当前日期
//        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);

        // 显示结果
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("当前日期的最早时间: " + startOfDay.format(formatter));
    }
}
