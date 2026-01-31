package com.parkingmanage.util;

import java.util.HashMap;
import java.util.Map;

/**
 * ACMS数据转换工具类
 * 用于将ACMS接口中的数字字段转换为对应的文字描述
 * 
 * @author lzx
 * @since 2024-10-24
 */
public class AcmsDataConverter {
    
    /**
     * 车牌颜色映射
     */
    private static final Map<String, String> CAR_LICENSE_COLOR_MAP = new HashMap<>();
    static {
        CAR_LICENSE_COLOR_MAP.put("0", "黑色");
        CAR_LICENSE_COLOR_MAP.put("1", "白色");
        CAR_LICENSE_COLOR_MAP.put("2", "红色");
        CAR_LICENSE_COLOR_MAP.put("3", "蓝色");
        CAR_LICENSE_COLOR_MAP.put("4", "黄色");
        CAR_LICENSE_COLOR_MAP.put("5", "绿色");
        CAR_LICENSE_COLOR_MAP.put("6", "其他");
    }
    
    /**
     * 车辆类型映射
     */
    private static final Map<String, String> CAR_TYPE_MAP = new HashMap<>();
    static {
        CAR_TYPE_MAP.put("1", "小型车");
        CAR_TYPE_MAP.put("2", "大型车");
        CAR_TYPE_MAP.put("3", "摩托车");
    }
    
    /**
     * 进出类型映射
     */
    private static final Map<String, String> ENTER_LEAVE_TYPE_MAP = new HashMap<>();
    static {
        ENTER_LEAVE_TYPE_MAP.put("1", "正常进出");
        ENTER_LEAVE_TYPE_MAP.put("2", "免费进出");
        ENTER_LEAVE_TYPE_MAP.put("3", "异常进出");
    }
    
    /**
     * VIP类型映射（根据实际业务需求调整）
     */
    private static final Map<String, String> VIP_TYPE_MAP = new HashMap<>();
    static {
        VIP_TYPE_MAP.put("1", "月卡");
        VIP_TYPE_MAP.put("2", "临时卡");
        VIP_TYPE_MAP.put("3", "免费卡");
        VIP_TYPE_MAP.put("4", "VIP卡");
        VIP_TYPE_MAP.put("5", "访客车");
    }
    
    /**
     * 支付状态映射
     */
    private static final Map<String, String> PAY_STATUS_MAP = new HashMap<>();
    static {
        PAY_STATUS_MAP.put("0", "未支付");
        PAY_STATUS_MAP.put("1", "已支付");
        PAY_STATUS_MAP.put("2", "免费");
    }
    
    /**
     * 支付方式映射
     */
    private static final Map<String, String> PAYMENT_MODE_MAP = new HashMap<>();
    static {
        PAYMENT_MODE_MAP.put("1", "现金");
        PAYMENT_MODE_MAP.put("2", "微信");
        PAYMENT_MODE_MAP.put("3", "支付宝");
        PAYMENT_MODE_MAP.put("4", "银行卡");
        PAYMENT_MODE_MAP.put("5", "ETC");
        PAYMENT_MODE_MAP.put("6", "月卡");
        PAYMENT_MODE_MAP.put("7", "免费");
        PAYMENT_MODE_MAP.put("8", "优惠券");
        PAYMENT_MODE_MAP.put("9", "其他");
    }
    
    /**
     * 记录类型映射
     */
    private static final Map<String, String> RECORD_TYPE_MAP = new HashMap<>();
    static {
        RECORD_TYPE_MAP.put("1", "正常记录");
        RECORD_TYPE_MAP.put("2", "异常记录");
    }
    
    /**
     * 支付来源映射
     */
    private static final Map<String, String> PAY_ORIGIN_MAP = new HashMap<>();
    static {
        PAY_ORIGIN_MAP.put("1", "系统");
        PAY_ORIGIN_MAP.put("2", "人工");
    }
    
    /**
     * 是否纠错映射
     */
    private static final Map<String, String> IS_CORRECT_MAP = new HashMap<>();
    static {
        IS_CORRECT_MAP.put("0", "否");
        IS_CORRECT_MAP.put("1", "是");
    }
    
    /**
     * 转换车牌颜色
     * @param colorCode 颜色代码
     * @return 颜色描述
     */
    public static String convertCarLicenseColor(String colorCode) {
        return CAR_LICENSE_COLOR_MAP.getOrDefault(colorCode, "未知");
    }
    
    /**
     * 转换车辆类型
     * @param typeCode 类型代码
     * @return 类型描述
     */
    public static String convertCarType(String typeCode) {
        return CAR_TYPE_MAP.getOrDefault(typeCode, "未知");
    }
    
    /**
     * 转换进出类型
     * @param typeCode 类型代码
     * @return 类型描述
     */
    public static String convertEnterLeaveType(String typeCode) {
        return ENTER_LEAVE_TYPE_MAP.getOrDefault(typeCode, "未知");
    }
    
    /**
     * 转换VIP类型
     * @param vipTypeCode VIP类型代码
     * @return VIP类型描述
     */
    public static String convertVipType(String vipTypeCode) {
        return VIP_TYPE_MAP.getOrDefault(vipTypeCode, "未知");
    }
    
    /**
     * 转换支付状态
     * @param payStatusCode 支付状态代码
     * @return 支付状态描述
     */
    public static String convertPayStatus(String payStatusCode) {
        return PAY_STATUS_MAP.getOrDefault(payStatusCode, "未知");
    }
    
    /**
     * 转换支付方式
     * @param paymentModeCode 支付方式代码
     * @return 支付方式描述
     */
    public static String convertPaymentMode(String paymentModeCode) {
        return PAYMENT_MODE_MAP.getOrDefault(paymentModeCode, "未知");
    }
    
    /**
     * 转换记录类型
     * @param recordTypeCode 记录类型代码
     * @return 记录类型描述
     */
    public static String convertRecordType(String recordTypeCode) {
        return RECORD_TYPE_MAP.getOrDefault(recordTypeCode, "未知");
    }
    
    /**
     * 转换支付来源
     * @param payOriginCode 支付来源代码
     * @return 支付来源描述
     */
    public static String convertPayOrigin(String payOriginCode) {
        return PAY_ORIGIN_MAP.getOrDefault(payOriginCode, "未知");
    }
    
    /**
     * 转换是否纠错
     * @param isCorrectCode 是否纠错代码
     * @return 是否纠错描述
     */
    public static String convertIsCorrect(String isCorrectCode) {
        return IS_CORRECT_MAP.getOrDefault(isCorrectCode, "未知");
    }
}
