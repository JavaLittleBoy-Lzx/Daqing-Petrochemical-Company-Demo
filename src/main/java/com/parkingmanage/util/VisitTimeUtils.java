package com.parkingmanage.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 进出场时间工具类
 * 用于处理JSON格式的进出场时间数组
 * 
 * @author System
 */
@Slf4j
public class VisitTimeUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // 时间近似判断阈值：20秒（用于判断离场记录中的进场时间与进场记录中的进场时间是否匹配）
    private static final long TIME_APPROXIMATE_THRESHOLD = 20 * 1000; // 20秒，单位：毫秒
    
    // 重复记录判断阈值：1秒（用于判断是否为重复的进出场记录）
    private static final long TIME_DUPLICATE_THRESHOLD = 1 * 1000; // 1秒，单位：毫秒
    
    // 时间相差太远判断阈值：30分钟（用于异常情况判断）
    private static final long TIME_FAR_THRESHOLD = 30 * 60 * 1000; // 30分钟，单位：毫秒

    /**
     * 进出场时间记录
     */
    public static class VisitTimeRecord {
        private String enterTime;
        private String leaveTime;

        public VisitTimeRecord() {
        }

        public VisitTimeRecord(String enterTime, String leaveTime) {
            this.enterTime = enterTime;
            this.leaveTime = leaveTime;
        }

        public String getEnterTime() {
            return enterTime;
        }

        public void setEnterTime(String enterTime) {
            this.enterTime = enterTime;
        }

        public String getLeaveTime() {
            return leaveTime;
        }

        public void setLeaveTime(String leaveTime) {
            this.leaveTime = leaveTime;
        }
    }

    /**
     * 解析JSON字符串为进出场时间记录列表
     * 
     * @param jsonStr JSON字符串
     * @return 进出场时间记录列表
     */
    public static List<VisitTimeRecord> parseVisitTimes(String jsonStr) {
        List<VisitTimeRecord> records = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return records;
        }

        try {
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                VisitTimeRecord record = new VisitTimeRecord();
                record.setEnterTime(obj.getString("enterTime"));
                record.setLeaveTime(obj.getString("leaveTime"));
                records.add(record);
            }
        } catch (Exception e) {
            log.warn("⚠️ 解析进出场时间JSON失败: {}, 错误: {}", jsonStr, e.getMessage());
        }

        return records;
    }

    /**
     * 将进出场时间记录列表转换为JSON字符串
     * 
     * @param records 进出场时间记录列表
     * @return JSON字符串
     */
    public static String toJsonString(List<VisitTimeRecord> records) {
        if (records == null || records.isEmpty()) {
            return "[]";
        }

        try {
            return JSON.toJSONString(records);
        } catch (Exception e) {
            log.warn("⚠️ 转换进出场时间记录为JSON失败: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * 添加进场记录
     * 
     * @param jsonStr 现有的JSON字符串
     * @param enterTime 进场时间
     * @return 更新后的JSON字符串
     */
    public static String addEnterTime(String jsonStr, String enterTime) {
        List<VisitTimeRecord> records = parseVisitTimes(jsonStr);
        
        // 检查最后一个记录是否有离场时间
        if (!records.isEmpty()) {
            VisitTimeRecord lastRecord = records.get(records.size() - 1);
            if (lastRecord.getLeaveTime() == null || lastRecord.getLeaveTime().isEmpty()) {
                // 最后一个记录还没有离场时间，检查是否为重复记录
                if (isTimeApproximate(lastRecord.getEnterTime(), enterTime, TIME_DUPLICATE_THRESHOLD)) {
                    log.info("ℹ️ 检测到重复的进场记录，跳过: {}", enterTime);
                    return jsonStr; // 不添加重复记录
                }
            }
        }
        
        // 添加新的进场记录
        records.add(new VisitTimeRecord(enterTime, null));
        return toJsonString(records);
    }

    /**
     * 更新离场时间
     * 查找匹配的进场记录（leaveTime为null且enterTime近似），更新其leaveTime
     * 
     * @param jsonStr 现有的JSON字符串
     * @param enterTime 进场时间（用于匹配）
     * @param leaveTime 离场时间
     * @return 更新后的JSON字符串和是否找到匹配记录
     */
    public static UpdateLeaveTimeResult updateLeaveTime(String jsonStr, String enterTime, String leaveTime) {
        List<VisitTimeRecord> records = parseVisitTimes(jsonStr);
        boolean found = false;
        
        // 查找匹配的进场记录（leaveTime为null且enterTime近似）
        for (VisitTimeRecord record : records) {
            if ((record.getLeaveTime() == null || record.getLeaveTime().isEmpty()) 
                    && isTimeApproximate(record.getEnterTime(), enterTime, TIME_APPROXIMATE_THRESHOLD)) {
                record.setLeaveTime(leaveTime);
                found = true;
                log.info("✅ 找到匹配的进场记录，更新离场时间: enterTime={}, leaveTime={}", 
                    record.getEnterTime(), leaveTime);
                break;
            }
        }
        
        // 如果未找到匹配记录，添加新的完整记录
        if (!found) {
            records.add(new VisitTimeRecord(enterTime, leaveTime));
            log.info("ℹ️ 未找到匹配的进场记录，添加新的完整记录: enterTime={}, leaveTime={}", 
                enterTime, leaveTime);
        }
        
        return new UpdateLeaveTimeResult(toJsonString(records), found);
    }

    /**
     * 更新离场时间结果
     */
    public static class UpdateLeaveTimeResult {
        private String jsonStr;
        private boolean found;

        public UpdateLeaveTimeResult(String jsonStr, boolean found) {
            this.jsonStr = jsonStr;
            this.found = found;
        }

        public String getJsonStr() {
            return jsonStr;
        }

        public boolean isFound() {
            return found;
        }
    }

    /**
     * 校验并修正进场时间
     * 如果预约记录中的enterTime与离场记录中的enterTime相差太远，则修正
     * 
     * @param jsonStr 现有的JSON字符串
     * @param reservationEnterTime 预约记录中的进场时间
     * @param reportEnterTime 离场记录中的进场时间
     * @return 更新后的JSON字符串和是否进行了修正
     */
    public static CorrectEnterTimeResult correctEnterTime(String jsonStr, String reservationEnterTime, 
                                                          String reportEnterTime) {
        if (reservationEnterTime == null || reportEnterTime == null) {
            return new CorrectEnterTimeResult(jsonStr, false);
        }

        // 如果时间相差不远（在30分钟内），不修改
        if (isTimeApproximate(reservationEnterTime, reportEnterTime, TIME_FAR_THRESHOLD)) {
            return new CorrectEnterTimeResult(jsonStr, false);
        }

        // 时间相差太远，需要修正
        List<VisitTimeRecord> records = parseVisitTimes(jsonStr);
        boolean corrected = false;

        // 查找匹配的记录并修正进场时间
        for (VisitTimeRecord record : records) {
            if (isTimeApproximate(record.getEnterTime(), reservationEnterTime, TIME_APPROXIMATE_THRESHOLD)) {
                record.setEnterTime(reportEnterTime);
                corrected = true;
                log.warn("⚠️ 修正进场时间: 原值={}, 新值={}", reservationEnterTime, reportEnterTime);
                break;
            }
        }

        return new CorrectEnterTimeResult(toJsonString(records), corrected);
    }

    /**
     * 修正进场时间结果
     */
    public static class CorrectEnterTimeResult {
        private String jsonStr;
        private boolean corrected;

        public CorrectEnterTimeResult(String jsonStr, boolean corrected) {
            this.jsonStr = jsonStr;
            this.corrected = corrected;
        }

        public String getJsonStr() {
            return jsonStr;
        }

        public boolean isCorrected() {
            return corrected;
        }
    }

    /**
     * 判断两个时间是否近似（时间差在阈值内）
     * 
     * @param time1 时间1（格式：yyyy-MM-dd HH:mm:ss）
     * @param time2 时间2（格式：yyyy-MM-dd HH:mm:ss）
     * @param threshold 阈值（毫秒）
     * @return true-近似，false-不近似
     */
    public static boolean isTimeApproximate(String time1, String time2, long threshold) {
        if (time1 == null || time2 == null) {
            return false;
        }

        try {
            Date date1 = DATE_FORMAT.parse(time1);
            Date date2 = DATE_FORMAT.parse(time2);
            long diff = Math.abs(date1.getTime() - date2.getTime());
            return diff <= threshold;
        } catch (ParseException e) {
            log.warn("⚠️ 时间解析失败: time1={}, time2={}, 错误: {}", time1, time2, e.getMessage());
            return false;
        }
    }

    /**
     * 比较两个时间的大小
     * 
     * @param time1 时间1（格式：yyyy-MM-dd HH:mm:ss）
     * @param time2 时间2（格式：yyyy-MM-dd HH:mm:ss）
     * @return -1: time1 < time2, 0: time1 == time2, 1: time1 > time2
     */
    public static int compareTime(String time1, String time2) {
        if (time1 == null && time2 == null) {
            return 0;
        }
        if (time1 == null) {
            return -1;
        }
        if (time2 == null) {
            return 1;
        }

        try {
            Date date1 = DATE_FORMAT.parse(time1);
            Date date2 = DATE_FORMAT.parse(time2);
            return date1.compareTo(date2);
        } catch (ParseException e) {
            log.warn("⚠️ 时间解析失败: time1={}, time2={}, 错误: {}", time1, time2, e.getMessage());
            return 0;
        }
    }

    /**
     * 格式化时间为字符串
     * 
     * @param date 日期对象
     * @return 格式化后的时间字符串
     */
    public static String formatTime(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    /**
     * 解析时间字符串为Date对象
     * 
     * @param timeStr 时间字符串
     * @return Date对象
     */
    public static Date parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return DATE_FORMAT.parse(timeStr);
        } catch (ParseException e) {
            log.warn("⚠️ 时间解析失败: {}, 错误: {}", timeStr, e.getMessage());
            return null;
        }
    }
}

