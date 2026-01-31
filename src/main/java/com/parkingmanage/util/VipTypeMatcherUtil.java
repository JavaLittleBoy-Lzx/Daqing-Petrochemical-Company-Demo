package com.parkingmanage.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * VIP类型智能匹配工具类
 * 当Oracle中的VIP类型在AKE系统中不存在时，自动寻找包含最多匹配权限的VIP类型
 * 
 * 使用场景：
 * - Oracle VIP类型："化工西化肥西复合肥南炼油东VIP"
 * - AKE系统中只有："化工西VIP"、"化肥西VIP"、"复合肥南VIP"、"化工西化肥西VIP"等
 * - 算法会找到包含最多匹配权限的组合，如"化工西化肥西复合肥南VIP"
 */
@Slf4j
public class VipTypeMatcherUtil {
    
    /**
     * AKE系统中已知的VIP类型列表
     * 根据实际部署的门禁系统配置，这里列出所有实际存在的VIP类型
     */
    private static final List<String> KNOWN_AKE_VIP_TYPES = Arrays.asList(
        // 单门VIP
        "化工西VIP",
        "化肥西VIP",
        "复合肥南VIP",

        // 双门VIP组合
        "化工西化肥西VIP",
        "化工西复合肥南VIP",
        "化肥西复合肥南VIP",

        // 三门VIP组合
        "化工西化肥西复合肥南VIP"
    );
    
    /**
     * 智能匹配VIP类型
     * 从Oracle VIP类型名称中提取权限，然后在AKE已知VIP类型中找到包含最多匹配权限的类型
     * 
     * @param oracleVipTypeName Oracle中的VIP类型名称（如"化工西化肥西复合肥南炼油东VIP"）
     * @return 匹配的AKE VIP类型名称，如果无法匹配则返回null
     */
    public static String findBestMatchVipType(String oracleVipTypeName) {
        if (oracleVipTypeName == null || oracleVipTypeName.trim().isEmpty()) {
            log.warn("Oracle VIP类型名称为空，无法匹配");
            return null;
        }
        
        log.info("开始智能匹配VIP类型，Oracle类型: {}", oracleVipTypeName);
        
        // 1. 从Oracle VIP类型中提取权限集合
        Set<String> oraclePermissions = VipPermissionUtil.extractPermissionsFromVipType(oracleVipTypeName);
        
        if (oraclePermissions.isEmpty()) {
            log.warn("无法从Oracle VIP类型[{}]中提取权限", oracleVipTypeName);
            return null;
        }
        
        log.info("Oracle VIP权限: {}", oraclePermissions);
        
        // 2. 遍历所有已知的AKE VIP类型，找到包含最多匹配权限的类型
        String bestMatch = null;
        int maxMatchCount = 0;
        Set<String> bestMatchPermissions = null;
        
        for (String akeVipType : KNOWN_AKE_VIP_TYPES) {
            // 提取AKE VIP类型的权限
            Set<String> akePermissions = VipPermissionUtil.extractPermissionsFromVipType(akeVipType);
            
            // 计算匹配的权限数量（交集大小）
            Set<String> intersection = new HashSet<>(oraclePermissions);
            intersection.retainAll(akePermissions);
            int matchCount = intersection.size();
            
            log.debug("比较AKE VIP类型[{}]，权限: {}，匹配数: {}", 
                    akeVipType, akePermissions, matchCount);
            
            // 更新最佳匹配
            if (matchCount > maxMatchCount) {
                maxMatchCount = matchCount;
                bestMatch = akeVipType;
                bestMatchPermissions = akePermissions;
            }
        }
        
        // 3. 返回最佳匹配结果
        if (bestMatch != null && maxMatchCount > 0) {
            log.info("找到最佳匹配VIP类型: {}，匹配权限数: {}/{}，匹配权限: {}",
                    bestMatch, maxMatchCount, oraclePermissions.size(), bestMatchPermissions);
            
            // 如果匹配度较低，记录警告
            if (maxMatchCount < oraclePermissions.size()) {
                Set<String> unmatchedPermissions = new HashSet<>(oraclePermissions);
                unmatchedPermissions.removeAll(bestMatchPermissions);
                log.warn("部分权限未匹配: {}，这些权限在AKE系统中可能不可用", unmatchedPermissions);
            }
            
            return bestMatch;
        } else {
            log.warn("无法找到匹配的AKE VIP类型，Oracle权限: {}", oraclePermissions);
            return null;
        }
    }
    
    /**
     * 智能匹配VIP类型（带回退策略）
     * 如果无法找到匹配的VIP类型，则返回默认的单门VIP类型
     * 
     * @param oracleVipTypeName Oracle中的VIP类型名称
     * @param defaultVipType 默认VIP类型（如"化工西VIP"）
     * @return 匹配的AKE VIP类型名称，如果无法匹配则返回默认类型
     */
    public static String findBestMatchVipTypeWithFallback(String oracleVipTypeName, String defaultVipType) {
        String bestMatch = findBestMatchVipType(oracleVipTypeName);
        
        if (bestMatch != null) {
            return bestMatch;
        }
        
        log.warn("无法匹配VIP类型，使用默认类型: {}", defaultVipType);
        return defaultVipType;
    }
    
    /**
     * 智能匹配黑名单类型
     * 从Oracle门权限中提取有效的门（只保留威尔系统中存在的门），然后生成黑名单类型名称
     *
     * @param oraclePermissions Oracle门权限集合（如[复合肥南, 化肥西, 化三南, 炼油西一, 机修厂西一, 化工北]）
     * @return 匹配的黑名单类型名称（如"请停车检查（化工西化肥西复合肥南）"）
     */
    public static String findBestMatchBlacklistType(Set<String> oraclePermissions) {
        if (oraclePermissions == null || oraclePermissions.isEmpty()) {
            log.warn("Oracle门权限为空，无法生成黑名单类型");
            return null;
        }

        log.info("开始智能匹配黑名单类型，Oracle权限: {}", oraclePermissions);

        // 获取威尔系统中实际存在的门名称
        Set<String> validAkeGateNames = getValidAkeGateNames();
        log.info("威尔系统中有效的门名称: {}", validAkeGateNames);

        // 过滤出有效的门权限
        Set<String> validPermissions = new LinkedHashSet<>();
        for (String permission : oraclePermissions) {
            if (validAkeGateNames.contains(permission)) {
                validPermissions.add(permission);
                log.debug("门[{}]在威尔系统中有效，保留", permission);
            } else {
                log.debug("门[{}]在威尔系统中无效，跳过", permission);
            }
        }

        if (validPermissions.isEmpty()) {
            log.warn("没有找到任何有效的门权限，Oracle权限: {}，跳过添加黑名单", oraclePermissions);
            // 如果没有有效权限，返回null，跳过添加黑名单
            return null;
        }

        // 找到包含最多有效权限的已知黑名单类型
        String bestMatch = findBestMatchBlacklistTypeFromKnown(validPermissions);

        if (bestMatch != null) {
            log.info("找到最佳匹配黑名单类型: {}，有效权限: {}/{}",
                    bestMatch, validPermissions.size(), oraclePermissions.size());

            // 记录被过滤的权限
            Set<String> filteredPermissions = new HashSet<>(oraclePermissions);
            filteredPermissions.removeAll(validPermissions);
            if (!filteredPermissions.isEmpty()) {
                log.warn("以下权限在威尔系统中不存在，已过滤: {}", filteredPermissions);
            }

            return bestMatch;
        } else {
            // 如果没有找到完全匹配的组合，直接用有效权限生成黑名单类型
            String blacklistType = VipPermissionUtil.mergePermissionsToBlacklistType(validPermissions);
            log.info("未找到完全匹配的组合，使用有效权限生成黑名单类型: {}", blacklistType);
            return blacklistType;
        }
    }

    /**
     * 从已知黑名单类型中找到包含最多有效权限的类型
     */
    private static String findBestMatchBlacklistTypeFromKnown(Set<String> validPermissions) {
        String bestMatch = null;
        int maxMatchCount = 0;

        for (String vipType : KNOWN_AKE_VIP_TYPES) {
            // 将VIP类型转换为权限集合
            Set<String> vipPermissions = VipPermissionUtil.extractPermissionsFromVipType(vipType);

            // 计算匹配的权限数量
            Set<String> intersection = new HashSet<>(validPermissions);
            intersection.retainAll(vipPermissions);
            int matchCount = intersection.size();

            // 更新最佳匹配（优先选择匹配数量多且完全包含有效权限的）
            if (matchCount > maxMatchCount ||
                (matchCount == maxMatchCount && matchCount == validPermissions.size() &&
                 vipPermissions.containsAll(validPermissions))) {
                maxMatchCount = matchCount;
                bestMatch = vipType;
            }
        }

        // 如果找到最佳VIP类型，转换为黑名单类型
        if (bestMatch != null) {
            Set<String> bestPermissions = VipPermissionUtil.extractPermissionsFromVipType(bestMatch);
            return VipPermissionUtil.mergePermissionsToBlacklistType(bestPermissions);
        }

        return null;
    }

    /**
     * 智能匹配VIP类型
     * 从Oracle门权限中提取有效的门（只保留威尔系统中存在的门），然后生成VIP类型名称
     *
     * @param oraclePermissions Oracle门权限集合
     * @return 匹配的VIP类型名称
     */
    public static String findBestMatchVipTypeFromPermissions(Set<String> oraclePermissions) {
        if (oraclePermissions == null || oraclePermissions.isEmpty()) {
            log.warn("Oracle门权限为空，无法生成VIP类型");
            return null;
        }

        log.info("开始智能匹配VIP类型，Oracle权限: {}", oraclePermissions);

        // 获取威尔系统中实际存在的门名称
        Set<String> validAkeGateNames = getValidAkeGateNames();

        // 过滤出有效的门权限
        Set<String> validPermissions = new LinkedHashSet<>();
        Set<String> invalidPermissions = new HashSet<>();

        for (String permission : oraclePermissions) {
            if (validAkeGateNames.contains(permission)) {
                validPermissions.add(permission);
            } else {
                invalidPermissions.add(permission);
            }
        }

        if (!invalidPermissions.isEmpty()) {
            log.warn("以下权限在威尔系统中不存在，已过滤: {}", invalidPermissions);
        }

        if (validPermissions.isEmpty()) {
            log.warn("没有找到任何有效的门权限");
            return null;
        }

        log.info("有效权限: {}", validPermissions);

        // 找到包含最多有效权限的已知VIP类型
        String bestMatch = null;
        int maxMatchCount = 0;
        Set<String> bestMatchPermissions = null;

        for (String vipType : KNOWN_AKE_VIP_TYPES) {
            Set<String> vipPermissions = VipPermissionUtil.extractPermissionsFromVipType(vipType);

            Set<String> intersection = new HashSet<>(validPermissions);
            intersection.retainAll(vipPermissions);
            int matchCount = intersection.size();

            // 优先选择：1) 匹配数量多 2) 完全包含有效权限
            boolean isBetterMatch = (matchCount > maxMatchCount) ||
                (matchCount == maxMatchCount && matchCount == validPermissions.size() &&
                 vipPermissions.containsAll(validPermissions));

            if (isBetterMatch) {
                maxMatchCount = matchCount;
                bestMatch = vipType;
                bestMatchPermissions = vipPermissions;
            }
        }

        if (bestMatch != null) {
            log.info("找到最佳匹配VIP类型: {}，匹配权限数: {}/{}",
                    bestMatch, maxMatchCount, validPermissions.size());
            return bestMatch;
        } else {
            // 降级：直接用有效权限生成VIP类型
            String generatedType = VipPermissionUtil.mergePermissionsToVipType(validPermissions);
            log.warn("未找到完全匹配的组合，生成VIP类型: {}", generatedType);
            return generatedType;
        }
    }

    /**
     * 获取威尔系统中实际有效的门名称
     * 从已知的VIP类型中提取所有门名称
     */
    private static Set<String> getValidAkeGateNames() {
        Set<String> validGateNames = new LinkedHashSet<>();
        for (String vipType : KNOWN_AKE_VIP_TYPES) {
            Set<String> permissions = VipPermissionUtil.extractPermissionsFromVipType(vipType);
            validGateNames.addAll(permissions);
        }
        return validGateNames;
    }

    /**
     * 获取所有已知的AKE VIP类型列表
     *
     * @return AKE VIP类型列表
     */
    public static List<String> getKnownAkeVipTypes() {
        return new ArrayList<>(KNOWN_AKE_VIP_TYPES);
    }
    
    /**
     * 添加自定义的AKE VIP类型
     * 用于动态扩展已知VIP类型列表
     * 
     * @param vipType VIP类型名称
     */
    public static void addKnownVipType(String vipType) {
        if (vipType != null && !vipType.trim().isEmpty() && !KNOWN_AKE_VIP_TYPES.contains(vipType)) {
            KNOWN_AKE_VIP_TYPES.add(vipType);
            log.info("添加新的AKE VIP类型: {}", vipType);
        }
    }
    
    /**
     * 计算两个VIP类型的相似度（0-1之间）
     * 
     * @param vipType1 VIP类型1
     * @param vipType2 VIP类型2
     * @return 相似度分数（0-1），1表示完全相同，0表示完全不同
     */
    public static double calculateSimilarity(String vipType1, String vipType2) {
        Set<String> permissions1 = VipPermissionUtil.extractPermissionsFromVipType(vipType1);
        Set<String> permissions2 = VipPermissionUtil.extractPermissionsFromVipType(vipType2);
        
        if (permissions1.isEmpty() && permissions2.isEmpty()) {
            return 1.0;
        }
        
        if (permissions1.isEmpty() || permissions2.isEmpty()) {
            return 0.0;
        }
        
        // 计算Jaccard相似度：交集大小 / 并集大小
        Set<String> intersection = new HashSet<>(permissions1);
        intersection.retainAll(permissions2);
        
        Set<String> union = new HashSet<>(permissions1);
        union.addAll(permissions2);
        
        return (double) intersection.size() / union.size();
    }
}
