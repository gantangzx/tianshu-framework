package com.gantang.tianshu.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @ClassName DateUtil
 * @Description 基于java8的日期工具类
 * @Author yaxi.hu
 * @Date 2020/9/28 14:22
 **/
public class DateUtil extends cn.hutool.core.date.DateUtil {


    /**
     * @Author yaxi.hu
     * @Description 获取两个时间段内的日期
     * @Date 14:30 2020/9/28
     * @Param [start, end]
     * @return java.util.List<java.lang.String>
     **/
    public static List<String> getBetweenDate(String start, String end){
        List<String> list = new ArrayList<>();
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        long distance = ChronoUnit.DAYS.between(startDate, endDate);
        if (distance < 1) {
            return list;
        }
        Stream.iterate(startDate, d -> d.plusDays(1)).limit(distance + 1).forEach(f -> list.add(f.toString()));
        return list;
    }


    /**
     * @Author yaxi.hu
     * @Description 获取两个时间段内的日期
     * @Date 14:30 2020/9/28
     * @Param [start, end]
     * @return java.util.List<java.time.LocalDate>
     **/
    public static List<LocalDate> getBetweenLocalDate(String start, String end) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        long distance = ChronoUnit.DAYS.between(startDate, endDate);
        if (distance < 1) {
            return list;
        }
        Stream.iterate(startDate, d -> d.plusDays(1)).limit(distance + 1).forEach(list::add);
        return list;
    }



    /**
     * @Author yaxi.hu
     * @Description 获取两个时间段内的日期
     * @Date 14:31 2020/9/28
     * @Param [start, end]
     * @return java.util.List<java.time.LocalDate>
     **/
    public static List<LocalDate> getBetweenLocalDate(LocalDate start, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        long distance = ChronoUnit.DAYS.between(start, end);
        if (distance < 1) return Collections.emptyList();
        Stream.iterate(start, d -> d.plusDays(1)).limit(distance + 1).forEach(list::add);
        return list;
    }



    public static String getDateTimeStr(LocalDateTime starDateTime, LocalDateTime endDateTime) {
        if(starDateTime.isAfter(endDateTime)){
            return "";
        }
        LocalDateTime tempDateTime  = LocalDateTime.from(starDateTime);
        long day = tempDateTime.until(endDateTime, ChronoUnit.DAYS);
        tempDateTime = tempDateTime.plusDays(day);
        long hours = tempDateTime.until(endDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);
        long minutes = tempDateTime.until(endDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);
        long seconds = tempDateTime.until(endDateTime, ChronoUnit.SECONDS);
        return day+"天"+hours+"时"+minutes+"分"+seconds+"秒";
    }
}
