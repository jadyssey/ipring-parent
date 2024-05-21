package org.ipring.localdatetime;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class Main {
    public static void main(String[] args) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");


        // 获取当前的ZonedDateTime，使用系统默认时区
        ZonedDateTime now = ZonedDateTime.now();

        // 定义日期时间格式化器，包含时区

        // 将ZonedDateTime格式化为指定格式的字符串
        String formattedDateTime = now.format(formatter);
        System.out.println(formattedDateTime);
        
        // 如果需要使用特定时区，例如 'Asia/Shanghai'
        ZonedDateTime shanghaiTime = now.withZoneSameInstant(ZoneOffset.of("+4"));
        String formattedShanghaiTime = shanghaiTime.format(formatter);
        System.out.println(formattedShanghaiTime);


        long timeMillis = System.currentTimeMillis();


    }
}
