package com.parkingmanage.util;

import java.util.*;

/**
 * 门名称映射工具类
 * 负责将Oracle数据库中的完整门名称转换为ake系统使用的简化名称
 * 
 * Oracle数据库中的CQDMNAME（完整列表）：
 * 1. 炼油南门
 * 2. 炼油南一
 * 3. 炼油西一
 * 4. 炼油西门
 * 5. 炼油东门
 * 6. 化肥西门
 * 7. 化工西门
 * 8. 化工北门
 * 9. 化工东门
 * 10. 化三南门
 * 11. 复合肥南门
 * 12. 机修厂西一
 * 13. 二罐区正门
 * 
 * ake系统中使用的简化门名称：
 * - 化工西（对应：化工西门）
 * - 化肥西（对应：化肥西门）
 * - 复合肥南（对应：复合肥南门）
 * 
 * 映射规则：
 * - 去除"门"字后缀（如"化工西门" → "化工西"）
 * - 保留其他特殊名称不变（如"炼油南一"、"机修厂西一"、"二罐区正门"）
 */
public class GateNameMapper {
    
    /**
     * Oracle完整门名称到ake简化名称的映射表
     */
    private static final Map<String, String> GATE_NAME_MAP = new HashMap<>();
    
    static {
        // 初始化映射表
        GATE_NAME_MAP.put("炼油南门", "炼油南");
        GATE_NAME_MAP.put("炼油南一", "炼油南一");
        GATE_NAME_MAP.put("炼油西一", "炼油西一");
        GATE_NAME_MAP.put("炼油西门", "炼油西");
        GATE_NAME_MAP.put("炼油东门", "炼油东");
        GATE_NAME_MAP.put("化肥西门", "化肥西");
        GATE_NAME_MAP.put("化工西门", "化工西");
        GATE_NAME_MAP.put("化工北门", "化工北");
        GATE_NAME_MAP.put("化工东门", "化工东");
        GATE_NAME_MAP.put("化三南门", "化三南");
        GATE_NAME_MAP.put("复合肥南门", "复合肥南");
        GATE_NAME_MAP.put("机修厂西一", "机修厂西一");
        GATE_NAME_MAP.put("二罐区正门", "二罐区正门");
    }
    
    /**
     * 将Oracle完整门名称转换为ake简化名称
     * 
     * @param oracleGateName Oracle数据库中的完整门名称
     * @return ake系统使用的简化名称，如果未找到映射则返回原名称
     */
    public static String toAkeGateName(String oracleGateName) {
        if (oracleGateName == null || oracleGateName.trim().isEmpty()) {
            return oracleGateName;
        }
        
        String trimmedName = oracleGateName.trim();
        return GATE_NAME_MAP.getOrDefault(trimmedName, trimmedName);
    }
    
    /**
     * 批量转换门名称列表
     * 
     * @param oracleGateNames Oracle门名称列表
     * @return ake简化名称列表
     */
    public static List<String> toAkeGateNames(List<String> oracleGateNames) {
        if (oracleGateNames == null || oracleGateNames.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> akeNames = new ArrayList<>();
        for (String oracleName : oracleGateNames) {
            String akeName = toAkeGateName(oracleName);
            if (akeName != null && !akeName.isEmpty()) {
                akeNames.add(akeName);
            }
        }
        return akeNames;
    }
    
    /**
     * 检查是否为有效的Oracle门名称
     * 
     * @param oracleGateName Oracle门名称
     * @return true表示是已知的门名称
     */
    public static boolean isValidOracleGateName(String oracleGateName) {
        if (oracleGateName == null || oracleGateName.trim().isEmpty()) {
            return false;
        }
        return GATE_NAME_MAP.containsKey(oracleGateName.trim());
    }
    
    /**
     * 获取所有支持的Oracle门名称
     * 
     * @return Oracle门名称集合
     */
    public static Set<String> getAllOracleGateNames() {
        return new HashSet<>(GATE_NAME_MAP.keySet());
    }
    
    /**
     * 获取所有ake简化门名称
     * 
     * @return ake简化门名称集合
     */
    public static Set<String> getAllAkeGateNames() {
        return new HashSet<>(GATE_NAME_MAP.values());
    }
}
