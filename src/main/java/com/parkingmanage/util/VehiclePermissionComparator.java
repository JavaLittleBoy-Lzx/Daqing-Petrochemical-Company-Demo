package com.parkingmanage.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 车辆权限提取和比较工具类
 * 负责从VIP类型名称和黑名单类型名称中提取门权限，并进行比较
 * 
 * 核心功能：
 * 1. 从VIP类型名称提取门权限集合（去除"VIP"后缀）
 * 2. 从黑名单类型名称提取门权限集合（提取括号内内容）
 * 3. 实现权限集合比较方法（不考虑顺序）
 */
@Slf4j
public class VehiclePermissionComparator {
    
    /**
     * 从VIP类型名称提取门权限集合
     * 
     * 格式：简化名称1+简化名称2+...+"VIP"
     * 示例：
     * - "化工西VIP" → [化工西]
     * - "化工西化肥西VIP" → [化工西, 化肥西]
     * - "化工西化肥西复合肥南VIP" → [化工西, 化肥西, 复合肥南]
     * 
     * @param vipTypeName VIP类型名称
     * @return 门权限集合（ake简化名称）
     */
    public static Set<String> extractPermissionsFromVipTypeName(String vipTypeName) {
        if (vipTypeName == null || vipTypeName.trim().isEmpty()) {
            log.warn("VIP类型名称为空，无法提取权限");
            return Collections.emptySet();
        }
        
        String trimmedName = vipTypeName.trim();
        
        // 检查是否以"VIP"结尾
        if (!trimmedName.endsWith("VIP")) {
            log.warn("VIP类型名称格式不正确，不以'VIP'结尾: {}", vipTypeName);
            return Collections.emptySet();
        }
        
        // 去除"VIP"后缀，得到门名称组合
        String gateNamesStr = trimmedName.substring(0, trimmedName.length() - 3);
        
        if (gateNamesStr.isEmpty()) {
            log.warn("VIP类型名称中没有门名称: {}", vipTypeName);
            return Collections.emptySet();
        }
        
        // 识别所有可能的门名称关键词
        Set<String> permissions = extractGateNamesFromString(gateNamesStr);
        
        log.debug("从VIP类型名称[{}]提取权限: {}", vipTypeName, permissions);
        
        return permissions;
    }
    
    /**
     * 从黑名单类型名称提取门权限集合
     * 
     * 格式："请停车检查（"+简化名称1+简化名称2+...+"）"
     * 示例：
     * - "请停车检查（化工西）" → [化工西]
     * - "请停车检查（化工西化肥西）" → [化工西, 化肥西]
     * - "请停车检查（化工西化肥西复合肥南）" → [化工西, 化肥西, 复合肥南]
     * 
     * @param blacklistTypeName 黑名单类型名称
     * @return 门权限集合（ake简化名称）
     */
    public static Set<String> extractPermissionsFromBlacklistTypeName(String blacklistTypeName) {
        if (blacklistTypeName == null || blacklistTypeName.trim().isEmpty()) {
            log.warn("黑名单类型名称为空，无法提取权限");
            return Collections.emptySet();
        }
        
        String trimmedName = blacklistTypeName.trim();
        
        // 使用正则表达式提取括号内的内容
        Pattern pattern = Pattern.compile("请停车检查[（(](.+?)[）)]");
        Matcher matcher = pattern.matcher(trimmedName);
        
        if (!matcher.find()) {
            log.warn("黑名单类型名称格式不正确，无法提取括号内容: {}", blacklistTypeName);
            return Collections.emptySet();
        }
        
        String gateNamesStr = matcher.group(1);
        
        if (gateNamesStr.isEmpty()) {
            log.warn("黑名单类型名称括号内没有门名称: {}", blacklistTypeName);
            return Collections.emptySet();
        }
        
        // 识别所有可能的门名称关键词
        Set<String> permissions = extractGateNamesFromString(gateNamesStr);
        
        log.debug("从黑名单类型名称[{}]提取权限: {}", blacklistTypeName, permissions);
        
        return permissions;
    }
    
    /**
     * 从字符串中识别所有可能的门名称关键词
     * 使用贪婪匹配，优先匹配较长的门名称
     * 
     * @param gateNamesStr 门名称组合字符串
     * @return 门名称集合
     */
    private static Set<String> extractGateNamesFromString(String gateNamesStr) {
        Set<String> gateNames = new HashSet<>();
        
        // 获取所有已知的ake门名称，按长度降序排序（优先匹配较长的名称）
        List<String> allAkeGateNames = new ArrayList<>(GateNameMapper.getAllAkeGateNames());
        allAkeGateNames.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        String remaining = gateNamesStr;
        
        // 贪婪匹配：从左到右识别门名称
        while (!remaining.isEmpty()) {
            boolean matched = false;
            
            // 尝试匹配所有已知的门名称（从长到短）
            for (String gateName : allAkeGateNames) {
                if (remaining.startsWith(gateName)) {
                    gateNames.add(gateName);
                    remaining = remaining.substring(gateName.length());
                    matched = true;
                    break;
                }
            }
            
            // 如果没有匹配到任何门名称，跳过当前字符
            if (!matched) {
                log.warn("无法识别门名称，跳过字符: {}", remaining.charAt(0));
                remaining = remaining.substring(1);
            }
        }
        
        return gateNames;
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
        
        boolean equal = permissions1.equals(permissions2);
        
        log.debug("权限比较: {} vs {} = {}", permissions1, permissions2, equal);
        
        return equal;
    }
    
    /**
     * 比较Oracle权限和ake权限是否相等
     * 
     * @param oracleGateNames Oracle完整门名称列表
     * @param akeTypeName ake类型名称（VIP或黑名单）
     * @param isBlacklist 是否为黑名单类型
     * @return true表示权限相同，false表示权限不同
     */
    public static boolean compareOracleAndAkePermissions(
            List<String> oracleGateNames, 
            String akeTypeName, 
            boolean isBlacklist) {
        
        // 1. 转换Oracle门名称为ake简化名称
        List<String> akeGateNames = GateNameMapper.toAkeGateNames(oracleGateNames);
        Set<String> oraclePermissions = akeGateNames.stream()
                .distinct()
                .collect(Collectors.toSet());
        
        // 2. 从ake类型名称提取权限
        Set<String> akePermissions;
        if (isBlacklist) {
            akePermissions = extractPermissionsFromBlacklistTypeName(akeTypeName);
        } else {
            akePermissions = extractPermissionsFromVipTypeName(akeTypeName);
        }
        
        // 3. 比较权限集合
        return arePermissionsEqual(oraclePermissions, akePermissions);
    }
    
    /**
     * 获取权限差异描述
     * 
     * @param oraclePermissions Oracle权限集合
     * @param akePermissions ake权限集合
     * @return 差异描述
     */
    public static String getPermissionDifference(Set<String> oraclePermissions, Set<String> akePermissions) {
        if (oraclePermissions == null) {
            oraclePermissions = Collections.emptySet();
        }
        if (akePermissions == null) {
            akePermissions = Collections.emptySet();
        }
        
        // 计算差异
        Set<String> addedPermissions = new HashSet<>(oraclePermissions);
        addedPermissions.removeAll(akePermissions);
        
        Set<String> removedPermissions = new HashSet<>(akePermissions);
        removedPermissions.removeAll(oraclePermissions);
        
        StringBuilder diff = new StringBuilder();
        
        if (!addedPermissions.isEmpty()) {
            diff.append("新增权限: ").append(addedPermissions).append("; ");
        }
        
        if (!removedPermissions.isEmpty()) {
            diff.append("移除权限: ").append(removedPermissions).append("; ");
        }
        
        if (diff.length() == 0) {
            return "权限相同";
        }
        
        return diff.toString().trim();
    }
}
