package com.parkingmanage.service.ake.impl;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.service.ake.AkeRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AKE进出场记录服务实现类
 * 使用Map接收数据，灵活应对接口参数变化
 */
@Slf4j
@Service
public class AkeRecordServiceImpl implements AkeRecordService {

    @Override
    public void handleCarInRecord(Map<String, Object> data) {
        if (data == null) {
            log.warn("⚠️ 进场记录数据为空");
            return;
        }
        
        // 获取业务内容
        JSONObject bizContent = null;
        if (data.get("biz_content") instanceof JSONObject) {
            bizContent = (JSONObject) data.get("biz_content");
        } else if (data.get("biz_content") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bizMap = (Map<String, Object>) data.get("biz_content");
            bizContent = new JSONObject(bizMap);
        }
        
        if (bizContent == null) {
            log.warn("⚠️ 进场记录业务内容为空");
            return;
        }
        // 提取关键字段
        String carLicenseNumber = bizContent.getString("car_license_number");
        if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
            carLicenseNumber = bizContent.getString("enter_car_license_number");
        }
        String enterTime = bizContent.getString("enter_time");
        String enterChannelName = bizContent.getString("enter_channel_name");
        // 转换进出类型数字为文字
        String enterType = convertEnterLeaveTypeToString(bizContent.getString("enter_type"));
        // 转换VIP类型数字为文字
        String enterVipType = convertVipTypeToString(bizContent.getString("enter_vip_type"));
        // 转换车牌颜色数字为文字
        String enterCarLicenseColor = convertCarLicenseColorToString(
                bizContent.getString("enter_car_license_color"));
        // 转换车辆类型数字为文字
        String enterCarType = convertCarTypeToString(bizContent.getString("enter_car_type"));
        String enterCustomVipName = bizContent.getString("enter_custom_vip_name");
        String enterCarFullPicture = addImageUrlPrefix(bizContent.getString("enter_car_full_picture"));
        log.info("✅ 进场记录处理完成: 车牌={}", carLicenseNumber);
        // TODO: 后续可以在这里添加数据库保存逻辑
    }

    @Override
    public void handleCarOutRecord(Map<String, Object> data) {
        if (data == null) {
            log.warn("⚠️ 离场记录数据为空");
            return;
        }
        // 获取业务内容
        JSONObject bizContent = null;
        if (data.get("biz_content") instanceof JSONObject) {
            bizContent = (JSONObject) data.get("biz_content");
        } else if (data.get("biz_content") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bizMap = (Map<String, Object>) data.get("biz_content");
            bizContent = new JSONObject(bizMap);
        }
        
        if (bizContent == null) {
            log.warn("⚠️ 离场记录业务内容为空");
            return;
        }
        
        // 提取关键字段
        String carLicenseNumber = bizContent.getString("car_license_number");
        if (carLicenseNumber == null || carLicenseNumber.isEmpty()) {
            carLicenseNumber = bizContent.getString("leave_car_license_number");
        }
        
        String enterTime = bizContent.getString("enter_time");
        String leaveTime = bizContent.getString("leave_time");
        String enterChannelName = bizContent.getString("enter_channel_name");
        String leaveChannelName = bizContent.getString("leave_channel_name");
        
        // 转换进出类型数字为文字
        String enterType = convertEnterLeaveTypeToString(bizContent.getString("enter_type"));
        String leaveType = convertEnterLeaveTypeToString(bizContent.getString("leave_type"));
        
        // 转换VIP类型数字为文字
        String enterVipType = convertVipTypeToString(bizContent.getString("enter_vip_type"));
        String leaveVipType = convertVipTypeToString(bizContent.getString("leave_vip_type"));
        
        String leaveCustomVipName = bizContent.getString("leave_custom_vip_name");
        String amountReceivable = bizContent.getString("amount_receivable");
        
        // 转换车牌颜色数字为文字
        String enterCarLicenseColor = convertCarLicenseColorToString(
                bizContent.getString("enter_car_license_color"));
        String leaveCarLicenseColor = convertCarLicenseColorToString(
                bizContent.getString("leave_car_license_color"));
        
        // 转换车辆类型数字为文字
        String enterCarType = convertCarTypeToString(bizContent.getString("enter_car_type"));
        String leaveCarType = convertCarTypeToString(bizContent.getString("leave_car_type"));
        
        // 转换记录类型数字为文字
        String recordType = convertRecordTypeToString(bizContent.getString("record_type"));
        String remark = bizContent.getString("remark");
        
        // 处理停车时长
        String stoppingTime = "0秒";
        String stoppingTimeStr = bizContent.getString("stopping_time");
        if (stoppingTimeStr != null && !stoppingTimeStr.isEmpty()) {
            try {
                int stoppingTimeSeconds = Integer.parseInt(stoppingTimeStr);
                stoppingTime = formatParkingDuration(stoppingTimeSeconds);
                log.info("🕒 停车时长格式化: {}秒 -> {}", stoppingTimeSeconds, stoppingTime);
            } catch (NumberFormatException e) {
                log.warn("⚠️ 停车时长格式错误，无法转换为数字: {}", stoppingTimeStr);
            }
        }
        
        String leaveCarFullPicture = addImageUrlPrefix(bizContent.getString("leave_car_full_picture"));
        String enterCarFullPicture = addImageUrlPrefix(bizContent.getString("enter_car_full_picture"));
        
        log.info("✅ 离场记录处理完成: 车牌={}", carLicenseNumber);
        
        // TODO: 后续可以在这里添加数据库保存逻辑
    }

    /**
     * 将秒数转换为小时分钟秒格式
     * 
     * @param seconds 秒数
     * @return 格式化的时间字符串
     */
    private String formatParkingDuration(int seconds) {
        if (seconds <= 0) {
            return "0秒";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("小时");
        }
        if (minutes > 0) {
            result.append(minutes).append("分钟");
        }
        if (remainingSeconds > 0) {
            result.append(remainingSeconds).append("秒");
        }
        if (result.length() == 0) {
            return "0秒";
        }
        return result.toString();
    }

    /**
     * 转换VIP类型数字为文字
     * 
     * @param vipType VIP类型数字字符串
     * @return VIP类型文字描述
     */
    private String convertVipTypeToString(String vipType) {
        if (vipType == null || vipType.isEmpty()) {
            return "未定义";
        }
        try {
            int type = Integer.parseInt(vipType);
            switch (type) {
                case 0: return "未定义";
                case 1: return "临时车";
                case 2: return "本地VIP";
                case 3: return "第三方VIP";
                case 4: return "黑名单";
                case 5: return "访客";
                case 6: return "预定车辆";
                case 7: return "共享车位车辆";
                default: return "未定义";
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ VIP类型格式错误，无法转换为数字: {}", vipType);
            return vipType;
        }
    }

    /**
     * 转换车牌颜色数字为文字
     * 
     * @param carLicenseColor 车牌颜色数字字符串
     * @return 车牌颜色文字描述
     */
    private String convertCarLicenseColorToString(String carLicenseColor) {
        if (carLicenseColor == null || carLicenseColor.isEmpty()) {
            return "其他";
        }
        try {
            int color = Integer.parseInt(carLicenseColor);
            switch (color) {
                case 0: return "其他";
                case 1: return "蓝色";
                case 2: return "黄色";
                case 3: return "白色";
                case 4: return "黑色";
                case 5: return "绿色";
                default: return "其他";
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ 车牌颜色格式错误，无法转换为数字: {}", carLicenseColor);
            return carLicenseColor;
        }
    }

    /**
     * 转换进出类型数字为文字
     * 
     * @param enterLeaveType 进出类型数字字符串
     * @return 进出类型文字描述
     */
    private String convertEnterLeaveTypeToString(String enterLeaveType) {
        if (enterLeaveType == null || enterLeaveType.isEmpty()) {
            return "未确认";
        }
        try {
            int type = Integer.parseInt(enterLeaveType);
            switch (type) {
                case 0: return "未确认";
                case 1: return "自动放行";
                case 2: return "确认放行";
                case 3: return "异常放行";
                default: return "未确认";
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ 进出类型格式错误，无法转换为数字: {}", enterLeaveType);
            return enterLeaveType;
        }
    }

    /**
     * 转换车辆类型数字为文字
     * 
     * @param carType 车辆类型数字字符串
     * @return 车辆类型文字描述
     */
    private String convertCarTypeToString(String carType) {
        if (carType == null || carType.isEmpty()) {
            return "未定义";
        }
        try {
            int type = Integer.parseInt(carType);
            switch (type) {
                case 0: return "未定义";
                case 1: return "小型车";
                case 2: return "大型车";
                case 3: return "摩托车";
                case 4: return "电动车";
                case 5: return "货车";
                case 6: return "客车";
                case 7: return "特种车辆";
                default: return "未定义";
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ 车辆类型格式错误，无法转换为数字: {}", carType);
            return carType;
        }
    }

    /**
     * 转换记录类型数字为文字
     * 
     * @param recordType 记录类型数字字符串
     * @return 记录类型文字描述
     */
    private String convertRecordTypeToString(String recordType) {
        if (recordType == null || recordType.isEmpty()) {
            return "正常记录";
        }
        try {
            int type = Integer.parseInt(recordType);
            switch (type) {
                case 0: return "未定义";
                case 1: return "有牌车";
                case 2: return "无牌车";
                case 3: return "遮挡车";
                case 4: return "非汽车";
                case 5: return "误触发";
                default: return "正常记录";
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ 记录类型格式错误，无法转换为数字: {}", recordType);
            return recordType;
        }
    }

    /**
     * 为图片URL添加前缀
     * 
     * @param imageUrl 原始图片URL
     * @return 带前缀的完整URL
     */
    private String addImageUrlPrefix(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        // 如果URL已经包含前缀，直接返回
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        // 添加前缀（根据实际情况修改）
        return "http://11.114.34.28:8092" + imageUrl;
    }
}
