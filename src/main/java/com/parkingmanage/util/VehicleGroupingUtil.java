package com.parkingmanage.util;

import com.parkingmanage.entity.GroupedVehicleInfo;
import com.parkingmanage.entity.OracleVehicleInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆数据分组工具类
 * 
 * 功能：将从Oracle获取的车辆数据按车牌号（CPH）分组
 * 
 * 设计说明：
 * - 同一车牌可能有多条记录（不同CQDM）
 * - 每条记录代表该车辆在一个厂区的权限
 * - 需要将同一车牌的所有记录合并为一个GroupedVehicleInfo对象
 * - 保持原始数据的顺序（按CZSJ排序）
 * 
 * Requirements: 5.1
 */
@Slf4j
public class VehicleGroupingUtil {
    
    /**
     * 按车牌号分组车辆数据
     * 
     * 输入：从Oracle获取的车辆数据列表（已按CZSJ排序）
     * 输出：按车牌号分组后的车辆信息列表
     * 
     * 分组规则：
     * 1. 使用车牌号（plateNumber）作为分组键
     * 2. 同一车牌的多条记录合并为一个GroupedVehicleInfo
     * 3. 提取所有记录的CQDMNAME到orgNames列表
     * 4. 基本信息（车主、有效期等）取第一条记录的值
     * 5. 保持原始顺序（LinkedHashMap）
     * 
     * @param vehicles 原始车辆数据列表（已按CZSJ排序）
     * @return 分组后的车辆信息列表
     */
    public static List<GroupedVehicleInfo> groupByPlateNumber(List<OracleVehicleInfo> vehicles) {
        log.info("========== 开始车辆数据分组 ==========");
        
        if (vehicles == null || vehicles.isEmpty()) {
            log.info("车辆数据为空，无需分组");
            return new ArrayList<>();
        }
        
        log.info("输入车辆记录数: {}", vehicles.size());
        
        // 使用LinkedHashMap保持插入顺序
        Map<String, GroupedVehicleInfo> groupedMap = new LinkedHashMap<>();
        
        // 遍历所有车辆记录，按车牌号分组
        for (OracleVehicleInfo vehicle : vehicles) {
            String plateNumber = vehicle.getPlateNumber();
            
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                log.warn("车辆记录缺少车牌号，跳过: {}", vehicle);
                continue;
            }
            
            // 获取或创建分组对象
            GroupedVehicleInfo grouped = groupedMap.get(plateNumber);
            if (grouped == null) {
                grouped = new GroupedVehicleInfo();
                groupedMap.put(plateNumber, grouped);
                log.debug("创建新分组: 车牌号={}", plateNumber);
            }
            
            // 添加记录到分组
            grouped.addRecord(vehicle);
            log.debug("添加记录到分组: 车牌号={}, 厂区={}", plateNumber, vehicle.getOrgName());
        }
        
        // 转换为列表
        List<GroupedVehicleInfo> result = new ArrayList<>(groupedMap.values());
        
        // 输出分组统计
        log.info("========== 车辆数据分组完成 ==========");
        log.info("分组后车辆数: {}", result.size());
        log.info("分组详情:");
        
        int totalRecords = 0;
        int singleRecordCount = 0;
        int multiRecordCount = 0;
        
        for (GroupedVehicleInfo grouped : result) {
            int recordCount = grouped.getRecordCount();
            totalRecords += recordCount;
            
            if (recordCount == 1) {
                singleRecordCount++;
            } else {
                multiRecordCount++;
                log.info("  车牌[{}]: {} 条记录, 厂区: {}", 
                        grouped.getPlateNumber(), recordCount, grouped.getOrgNames());
            }
        }
        
        log.info("统计: 单记录车辆={}, 多记录车辆={}, 总记录数={}", 
                singleRecordCount, multiRecordCount, totalRecords);
        
        return result;
    }
    
    /**
     * 获取分组统计信息（用于调试）
     * 
     * @param groupedVehicles 分组后的车辆列表
     * @return 统计信息字符串
     */
    public static String getGroupingStatistics(List<GroupedVehicleInfo> groupedVehicles) {
        if (groupedVehicles == null || groupedVehicles.isEmpty()) {
            return "无车辆数据";
        }
        
        int totalVehicles = groupedVehicles.size();
        int totalRecords = 0;
        int maxRecords = 0;
        String maxRecordsPlate = "";
        
        for (GroupedVehicleInfo grouped : groupedVehicles) {
            int recordCount = grouped.getRecordCount();
            totalRecords += recordCount;
            
            if (recordCount > maxRecords) {
                maxRecords = recordCount;
                maxRecordsPlate = grouped.getPlateNumber();
            }
        }
        
        double avgRecords = (double) totalRecords / totalVehicles;
        
        return String.format("车辆数=%d, 总记录数=%d, 平均记录数=%.2f, 最多记录数=%d(车牌:%s)", 
                totalVehicles, totalRecords, avgRecords, maxRecords, maxRecordsPlate);
    }
}
