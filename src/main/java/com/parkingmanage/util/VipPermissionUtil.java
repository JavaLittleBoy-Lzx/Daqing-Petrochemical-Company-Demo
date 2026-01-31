package com.parkingmanage.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * VIP权限工具类
 * 负责从VIP类型名称和黑名单类型名称中提取门权限信息，并进行权限比较
 * 
 * VIP类型名称格式：简化名称1+简化名称2+...+"VIP"
 * 示例：
 * - "化工西VIP" → [化工西]
 * - "化工西化肥西VIP" → [化工西, 化肥西]
 * - "化工西化肥西复合肥南VIP" → [化工西, 化肥西, 复合肥南]
 * 
 * 黑名单类型名称格式："请停车检查（"+简化名称1+简化名称2+...+"）"
 * 示例：
 * - "请停车检查（化工西）" → [化工西]
 * - "请停车检查（化工西化肥西）" → [化工西, 化肥西]
 * - "请停车检查（化工西化肥西复合肥南）" → [化工西, 化肥西, 复合肥南]
 */
public class VipPermissionUtil {
    
    /** VIP类型名称后缀 */
    private static final String VIP_SUFFIX = "VIP";
    
    /** 黑名单类型名称前缀 */
    private static final String BLACKLIST_PREFIX = "请停车检查（";
    
    /** 黑名单类型名称后缀 */
    private static final String BLACKLIST_SUFFIX = "）";
    
    /**
     * 从VIP类型名称中提取门权限集合
     * 
     * @param vipTypeName VIP类型名称（如"化工西化肥西VIP"）
     * @return 门权限集合（如[化工西, 化肥西]），如果无法提取则返回空集合
     */
    public static Set<String> extractPermissionsFromVipType(String vipTypeName) {
        if (vipTypeName == null || vipTypeName.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        String trimmed = vipTypeName.trim();
        
        // 去除"VIP"后缀
        if (!trimmed.endsWith(VIP_SUFFIX)) {
            return Collections.emptySet();
        }
        
        String gateNames = trimmed.substring(0, trimmed.length() - VIP_SUFFIX.length());
        
        // 识别所有可能的门名称关键词
        return extractGateNamesFromString(gateNames);
    }
    
    /**
     * 从黑名单类型名称中提取门权限集合
     * 
     * @param blacklistTypeName 黑名单类型名称（如"请停车检查（化工西化肥西）"）
     * @return 门权限集合（如[化工西, 化肥西]），如果无法提取则返回空集合
     */
    public static Set<String> extractPermissionsFromBlacklistType(String blacklistTypeName) {
        if (blacklistTypeName == null || blacklistTypeName.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        String trimmed = blacklistTypeName.trim();
        
        // 提取括号内的内容
        if (!trimmed.startsWith(BLACKLIST_PREFIX) || !trimmed.endsWith(BLACKLIST_SUFFIX)) {
            return Collections.emptySet();
        }
        
        String gateNames = trimmed.substring(
                BLACKLIST_PREFIX.length(), 
                trimmed.length() - BLACKLIST_SUFFIX.length());
        
        // 识别所有可能的门名称关键词
        return extractGateNamesFromString(gateNames);
    }
    
    /**
     * 从字符串中提取门名称集合
     * 使用贪婪匹配算法，优先匹配较长的门名称
     * 
     * @param gateNamesStr 门名称组合字符串（如"化工西化肥西复合肥南"）
     * @return 门名称集合
     */
    private static Set<String> extractGateNamesFromString(String gateNamesStr) {
        if (gateNamesStr == null || gateNamesStr.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> result = new LinkedHashSet<>();
        Set<String> allAkeGateNames = GateNameMapper.getAllAkeGateNames();
        
        // 按长度降序排序，优先匹配较长的门名称
        List<String> sortedGateNames = allAkeGateNames.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());
        
        String remaining = gateNamesStr;
        
        // 贪婪匹配
        while (!remaining.isEmpty()) {
            boolean matched = false;
            
            for (String gateName : sortedGateNames) {
                if (remaining.startsWith(gateName)) {
                    result.add(gateName);
                    remaining = remaining.substring(gateName.length());
                    matched = true;
                    break;
                }
            }
            
            if (!matched) {
                // 无法匹配，跳过一个字符
                remaining = remaining.substring(1);
            }
        }
        
        return result;
    }
    
    /**
     * 比较两个权限集合是否相等（不考虑顺序）
     * 
     * @param permissions1 权限集合1
     * @param permissions2 权限集合2
     * @return true表示权限相同，false表示权限不同
     */
    public static boolean arePermissionsEqual(Set<String> permissions1, Set<String> permissions2) {
        if (permissions1 == null && permissions2 == null) {
            return true;
        }
        if (permissions1 == null || permissions2 == null) {
            return false;
        }
        return permissions1.equals(permissions2);
    }
    
    /**
     * 从Oracle门名称列表提取权限集合（转换为ake简化名称）
     * 
     * @param oracleGateNames Oracle门名称列表
     * @return ake简化名称集合
     */
    public static Set<String> extractPermissionsFromOracleGateNames(List<String> oracleGateNames) {
        if (oracleGateNames == null || oracleGateNames.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> permissions = new LinkedHashSet<>();
        for (String oracleName : oracleGateNames) {
            String akeName = GateNameMapper.toAkeGateName(oracleName);
            if (akeName != null && !akeName.isEmpty()) {
                permissions.add(akeName);
            }
        }
        return permissions;
    }
    
    /**
     * 合并门权限集合为VIP类型名称
     * 格式：简化名称1+简化名称2+...+"VIP"
     * 
     * @param permissions 门权限集合
     * @return VIP类型名称
     */
    public static String mergePermissionsToVipType(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        
        // 保持顺序，直接拼接
        StringBuilder sb = new StringBuilder();
        for (String permission : permissions) {
            sb.append(permission);
        }
        sb.append(VIP_SUFFIX);
        
        return sb.toString();
    }
    
    /**
     * 合并门权限集合为黑名单类型名称
     * 格式："请停车检查（"+简化名称1+简化名称2+...+"）"
     * 
     * @param permissions 门权限集合
     * @return 黑名单类型名称
     */
    public static String mergePermissionsToBlacklistType(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        
        // 保持顺序，直接拼接
        StringBuilder sb = new StringBuilder(BLACKLIST_PREFIX);
        for (String permission : permissions) {
            sb.append(permission);
        }
        sb.append(BLACKLIST_SUFFIX);
        
        return sb.toString();
    }
}
