package com.parkingmanage.util;

import com.parkingmanage.entity.OracleVehicleInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆权限合并工具类
 * 负责将同一车牌的多个门权限合并为一个类型名称
 * 
 * 核心功能：
 * 1. 提取同一车牌所有记录的CQDMNAME（Oracle完整门名称）
 * 2. 转换为ake简化名称（使用GateNameMapper）
 * 3. 去重并排序
 * 4. 生成VIP类型名称：简化名称1+简化名称2+...+"VIP"
 * 5. 生成黑名单类型名称："请停车检查（"+简化名称1+简化名称2+...+"）"
 */
@Slf4j
public class VehiclePermissionMerger {
    
    /**
     * 从车辆记录列表中提取并合并门权限
     * 
     * @param vehicleRecords 同一车牌的多条车辆记录
     * @return 合并后的ake简化门名称列表（已去重并排序）
     */
    public static List<String> extractAndMergeGatePermissions(List<OracleVehicleInfo> vehicleRecords) {
        if (vehicleRecords == null || vehicleRecords.isEmpty()) {
            log.warn("车辆记录列表为空，无法提取权限");
            return Collections.emptyList();
        }
        
        // 1. 提取所有记录的CQDMNAME（Oracle完整门名称）
        List<String> oracleGateNames = vehicleRecords.stream()
                .map(OracleVehicleInfo::getOrgName)  // CQDMNAME存储在orgName字段
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());
        
        if (oracleGateNames.isEmpty()) {
            log.warn("车辆记录中没有有效的门名称");
            return Collections.emptyList();
        }
        
        // 2. 转换为ake简化名称
        List<String> akeGateNames = GateNameMapper.toAkeGateNames(oracleGateNames);
        
        // 3. 去重并排序
        List<String> mergedGateNames = akeGateNames.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        log.debug("权限合并完成：Oracle门名称={}, ake简化名称={}", oracleGateNames, mergedGateNames);
        
        return mergedGateNames;
    }
    
    /**
     * 生成VIP类型名称
     * 格式：简化名称1+简化名称2+...+"VIP"
     * 
     * 示例：
     * - 单门：化工西VIP
     * - 双门：化工西化肥西VIP
     * - 三门：化工西化肥西复合肥南VIP
     * 
     * @param vehicleRecords 同一车牌的多条车辆记录
     * @return VIP类型名称
     */
    public static String generateVipTypeName(List<OracleVehicleInfo> vehicleRecords) {
        List<String> gateNames = extractAndMergeGatePermissions(vehicleRecords);
        
        if (gateNames.isEmpty()) {
            log.warn("无法生成VIP类型名称：没有有效的门权限");
            return null;
        }
        
        // 拼接门名称 + "VIP"
        String vipTypeName = String.join("", gateNames) + "VIP";
        
        log.debug("生成VIP类型名称：{}", vipTypeName);
        
        return vipTypeName;
    }
    
    /**
     * 生成黑名单类型名称
     * 格式："请停车检查（"+简化名称1+简化名称2+...+"）"
     * 
     * 示例：
     * - 单门：请停车检查（化工西）
     * - 双门：请停车检查（化工西化肥西）
     * - 三门：请停车检查（化工西化肥西复合肥南）
     * 
     * @param vehicleRecords 同一车牌的多条车辆记录
     * @return 黑名单类型名称
     */
    public static String generateBlacklistTypeName(List<OracleVehicleInfo> vehicleRecords) {
        List<String> gateNames = extractAndMergeGatePermissions(vehicleRecords);
        
        if (gateNames.isEmpty()) {
            log.warn("无法生成黑名单类型名称：没有有效的门权限");
            return null;
        }
        
        // 拼接 "请停车检查（" + 门名称 + "）"
        String blacklistTypeName = "请停车检查（" + String.join("", gateNames) + "）";
        
        log.debug("生成黑名单类型名称：{}", blacklistTypeName);
        
        return blacklistTypeName;
    }
    
    /**
     * 从Oracle门名称列表生成VIP类型名称
     * 
     * @param oracleGateNames Oracle完整门名称列表
     * @return VIP类型名称
     */
    public static String generateVipTypeNameFromOracleNames(List<String> oracleGateNames) {
        if (oracleGateNames == null || oracleGateNames.isEmpty()) {
            return null;
        }
        
        // 转换为ake简化名称
        List<String> akeGateNames = GateNameMapper.toAkeGateNames(oracleGateNames);
        
        // 去重并排序
        List<String> mergedGateNames = akeGateNames.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        if (mergedGateNames.isEmpty()) {
            return null;
        }
        
        return String.join("", mergedGateNames) + "VIP";
    }
    
    /**
     * 从Oracle门名称列表生成黑名单类型名称
     * 
     * @param oracleGateNames Oracle完整门名称列表
     * @return 黑名单类型名称
     */
    public static String generateBlacklistTypeNameFromOracleNames(List<String> oracleGateNames) {
        if (oracleGateNames == null || oracleGateNames.isEmpty()) {
            return null;
        }
        
        // 转换为ake简化名称
        List<String> akeGateNames = GateNameMapper.toAkeGateNames(oracleGateNames);
        
        // 去重并排序
        List<String> mergedGateNames = akeGateNames.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        if (mergedGateNames.isEmpty()) {
            return null;
        }
        
        return "请停车检查（" + String.join("", mergedGateNames) + "）";
    }
}
