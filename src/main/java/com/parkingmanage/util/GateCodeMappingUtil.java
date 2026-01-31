package com.parkingmanage.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 大门编码映射工具类
 * 
 * 将Oracle中的CQDM（厂区代码）映射到大门全称
 * 
 * 编码规则：
 * - 厂区(2位)、大门(4位)编码
 * - 例如：0301 = 化工西门
 */
public class GateCodeMappingUtil {

    /**
     * CQDM到大门全称的映射表
     * Key: CQDM编码（4位）
     * Value: 大门全称
     */
    private static final Map<String, String> CQDM_TO_GATE_NAME = new HashMap<>();
    
    /**
     * 厂区编码到厂区名称的映射表
     * Key: 厂区编码（2位）
     * Value: 厂区名称
     */
    private static final Map<String, String> AREA_CODE_TO_NAME = new HashMap<>();

    static {
        // 初始化厂区编码映射
        AREA_CODE_TO_NAME.put("01", "炼油");
        AREA_CODE_TO_NAME.put("02", "化肥");
        AREA_CODE_TO_NAME.put("03", "化工");
        AREA_CODE_TO_NAME.put("04", "化三");
        AREA_CODE_TO_NAME.put("05", "复合肥");
        AREA_CODE_TO_NAME.put("09", "机修厂");
        AREA_CODE_TO_NAME.put("10", "指挥内部");
        AREA_CODE_TO_NAME.put("11", "二罐区");
        AREA_CODE_TO_NAME.put("12", "一罐区");
        AREA_CODE_TO_NAME.put("13", "北油库");
        AREA_CODE_TO_NAME.put("14", "化肥北");
        AREA_CODE_TO_NAME.put("15", "指挥外部");
        
        // 初始化大门编码映射
        // 炼油厂区
        CQDM_TO_GATE_NAME.put("0101", "炼油南门");
        CQDM_TO_GATE_NAME.put("0102", "炼油南一");
        CQDM_TO_GATE_NAME.put("0104", "炼油西一");
        CQDM_TO_GATE_NAME.put("0105", "炼油西门");
        CQDM_TO_GATE_NAME.put("0106", "炼油东门");
        
        // 化肥厂区
        CQDM_TO_GATE_NAME.put("0201", "化肥西门");
        CQDM_TO_GATE_NAME.put("0202", "化肥北门");
        
        // 化工厂区
        CQDM_TO_GATE_NAME.put("0301", "化工西门");
        CQDM_TO_GATE_NAME.put("0302", "化工北门");
        CQDM_TO_GATE_NAME.put("0304", "化工东门");
        
        // 化三厂区
        CQDM_TO_GATE_NAME.put("0401", "化三南门");
        CQDM_TO_GATE_NAME.put("0402", "化三西二");
        
        // 复合肥厂区
        CQDM_TO_GATE_NAME.put("0501", "复合肥南门");
        
        // 机修厂
        CQDM_TO_GATE_NAME.put("0901", "机修厂西一");
        
        // 指挥内部
        CQDM_TO_GATE_NAME.put("1001", "指挥内部正门");
        
        // 二罐区
        CQDM_TO_GATE_NAME.put("1101", "二罐区正门");
        
        // 一罐区
        CQDM_TO_GATE_NAME.put("1201", "一罐区正门");
        
        // 北油库
        CQDM_TO_GATE_NAME.put("1301", "北油库正门");
        
        // 化肥北
        CQDM_TO_GATE_NAME.put("1401", "化肥北北门");
        
        // 指挥外部
        CQDM_TO_GATE_NAME.put("1501", "指挥外部正门");
    }

    /**
     * 根据CQDM编码获取大门全称
     * 
     * @param cqdm CQDM编码（4位）
     * @return 大门全称，如果未找到返回null
     */
    public static String getGateNameByCqdm(String cqdm) {
        if (cqdm == null || cqdm.trim().isEmpty()) {
            return null;
        }
        return CQDM_TO_GATE_NAME.get(cqdm.trim());
    }

    /**
     * 根据厂区编码获取厂区名称
     * 
     * @param areaCode 厂区编码（2位）
     * @return 厂区名称，如果未找到返回null
     */
    public static String getAreaNameByCode(String areaCode) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            return null;
        }
        return AREA_CODE_TO_NAME.get(areaCode.trim());
    }

    /**
     * 从CQDM编码中提取厂区编码
     * 
     * @param cqdm CQDM编码（4位）
     * @return 厂区编码（2位），如果格式不正确返回null
     */
    public static String extractAreaCode(String cqdm) {
        if (cqdm == null || cqdm.trim().length() < 2) {
            return null;
        }
        return cqdm.trim().substring(0, 2);
    }

    /**
     * 获取所有CQDM到大门名称的映射
     * 
     * @return 映射表的副本
     */
    public static Map<String, String> getAllGateMappings() {
        return new HashMap<>(CQDM_TO_GATE_NAME);
    }

    /**
     * 检查CQDM编码是否有效
     * 
     * @param cqdm CQDM编码
     * @return 是否有效
     */
    public static boolean isValidCqdm(String cqdm) {
        return cqdm != null && CQDM_TO_GATE_NAME.containsKey(cqdm.trim());
    }
}
