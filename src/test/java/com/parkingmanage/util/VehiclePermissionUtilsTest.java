package com.parkingmanage.util;

import com.parkingmanage.entity.OracleVehicleInfo;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆权限工具类测试
 */
class VehiclePermissionUtilsTest {
    
    @Test
    void testGateNameMapper() {
        // 测试门名称映射
        assertEquals("化工西", GateNameMapper.toAkeGateName("化工西门"));
        assertEquals("化肥西", GateNameMapper.toAkeGateName("化肥西门"));
        assertEquals("复合肥南", GateNameMapper.toAkeGateName("复合肥南门"));
        assertEquals("炼油南一", GateNameMapper.toAkeGateName("炼油南一"));
        assertEquals("机修厂西一", GateNameMapper.toAkeGateName("机修厂西一"));
        assertEquals("二罐区正门", GateNameMapper.toAkeGateName("二罐区正门"));
        
        // 测试批量转换
        List<String> oracleNames = Arrays.asList("化工西门", "化肥西门", "复合肥南门");
        List<String> akeNames = GateNameMapper.toAkeGateNames(oracleNames);
        assertEquals(3, akeNames.size());
        assertTrue(akeNames.contains("化工西"));
        assertTrue(akeNames.contains("化肥西"));
        assertTrue(akeNames.contains("复合肥南"));
    }
    
    @Test
    void testVehiclePermissionMerger_SingleGate() {
        // 测试单个门权限
        OracleVehicleInfo vehicle = new OracleVehicleInfo();
        vehicle.setPlateNumber("黑A12345");
        vehicle.setOrgName("化工西门");
        
        List<OracleVehicleInfo> records = Collections.singletonList(vehicle);
        
        // 测试VIP类型名称生成
        String vipTypeName = VehiclePermissionMerger.generateVipTypeName(records);
        assertEquals("化工西VIP", vipTypeName);
        
        // 测试黑名单类型名称生成
        String blacklistTypeName = VehiclePermissionMerger.generateBlacklistTypeName(records);
        assertEquals("请停车检查（化工西）", blacklistTypeName);
    }
    
    @Test
    void testVehiclePermissionMerger_MultipleGates() {
        // 测试多个门权限
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOrgName("化工西门");
        
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber("黑A12345");
        vehicle2.setOrgName("化肥西门");
        
        List<OracleVehicleInfo> records = Arrays.asList(vehicle1, vehicle2);
        
        // 测试VIP类型名称生成（应该按字母顺序排序）
        String vipTypeName = VehiclePermissionMerger.generateVipTypeName(records);
        assertEquals("化工西化肥西VIP", vipTypeName);
        
        // 测试黑名单类型名称生成
        String blacklistTypeName = VehiclePermissionMerger.generateBlacklistTypeName(records);
        assertEquals("请停车检查（化工西化肥西）", blacklistTypeName);
    }
    
    @Test
    void testVehiclePermissionMerger_ThreeGates() {
        // 测试三个门权限
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOrgName("化工西门");
        
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber("黑A12345");
        vehicle2.setOrgName("化肥西门");
        
        OracleVehicleInfo vehicle3 = new OracleVehicleInfo();
        vehicle3.setPlateNumber("黑A12345");
        vehicle3.setOrgName("复合肥南门");
        
        List<OracleVehicleInfo> records = Arrays.asList(vehicle1, vehicle2, vehicle3);
        
        // 测试VIP类型名称生成
        String vipTypeName = VehiclePermissionMerger.generateVipTypeName(records);
        assertEquals("化工西化肥西复合肥南VIP", vipTypeName);
        
        // 测试黑名单类型名称生成
        String blacklistTypeName = VehiclePermissionMerger.generateBlacklistTypeName(records);
        assertEquals("请停车检查（化工西化肥西复合肥南）", blacklistTypeName);
    }
    
    @Test
    void testVehiclePermissionComparator_ExtractFromVipTypeName() {
        // 测试从VIP类型名称提取权限
        Set<String> permissions1 = VehiclePermissionComparator.extractPermissionsFromVipTypeName("化工西VIP");
        assertEquals(1, permissions1.size());
        assertTrue(permissions1.contains("化工西"));
        
        Set<String> permissions2 = VehiclePermissionComparator.extractPermissionsFromVipTypeName("化工西化肥西VIP");
        assertEquals(2, permissions2.size());
        assertTrue(permissions2.contains("化工西"));
        assertTrue(permissions2.contains("化肥西"));
        
        Set<String> permissions3 = VehiclePermissionComparator.extractPermissionsFromVipTypeName("化工西化肥西复合肥南VIP");
        assertEquals(3, permissions3.size());
        assertTrue(permissions3.contains("化工西"));
        assertTrue(permissions3.contains("化肥西"));
        assertTrue(permissions3.contains("复合肥南"));
    }
    
    @Test
    void testVehiclePermissionComparator_ExtractFromBlacklistTypeName() {
        // 测试从黑名单类型名称提取权限
        Set<String> permissions1 = VehiclePermissionComparator.extractPermissionsFromBlacklistTypeName("请停车检查（化工西）");
        assertEquals(1, permissions1.size());
        assertTrue(permissions1.contains("化工西"));
        
        Set<String> permissions2 = VehiclePermissionComparator.extractPermissionsFromBlacklistTypeName("请停车检查（化工西化肥西）");
        assertEquals(2, permissions2.size());
        assertTrue(permissions2.contains("化工西"));
        assertTrue(permissions2.contains("化肥西"));
        
        Set<String> permissions3 = VehiclePermissionComparator.extractPermissionsFromBlacklistTypeName("请停车检查（化工西化肥西复合肥南）");
        assertEquals(3, permissions3.size());
        assertTrue(permissions3.contains("化工西"));
        assertTrue(permissions3.contains("化肥西"));
        assertTrue(permissions3.contains("复合肥南"));
    }
    
    @Test
    void testVehiclePermissionComparator_ComparePermissions() {
        // 测试权限比较
        Set<String> permissions1 = new HashSet<>(Arrays.asList("化工西", "化肥西"));
        Set<String> permissions2 = new HashSet<>(Arrays.asList("化肥西", "化工西")); // 顺序不同
        Set<String> permissions3 = new HashSet<>(Arrays.asList("化工西", "复合肥南"));
        
        // 相同权限（不考虑顺序）
        assertTrue(VehiclePermissionComparator.arePermissionsEqual(permissions1, permissions2));
        
        // 不同权限
        assertFalse(VehiclePermissionComparator.arePermissionsEqual(permissions1, permissions3));
    }
    
    @Test
    void testVehiclePermissionComparator_CompareOracleAndAke() {
        // 测试Oracle和ake权限比较
        List<String> oracleNames = Arrays.asList("化工西门", "化肥西门");
        
        // VIP类型：权限相同
        assertTrue(VehiclePermissionComparator.compareOracleAndAkePermissions(
                oracleNames, "化工西化肥西VIP", false));
        
        // VIP类型：权限不同
        assertFalse(VehiclePermissionComparator.compareOracleAndAkePermissions(
                oracleNames, "化工西VIP", false));
        
        // 黑名单类型：权限相同
        assertTrue(VehiclePermissionComparator.compareOracleAndAkePermissions(
                oracleNames, "请停车检查（化工西化肥西）", true));
        
        // 黑名单类型：权限不同
        assertFalse(VehiclePermissionComparator.compareOracleAndAkePermissions(
                oracleNames, "请停车检查（化工西）", true));
    }
    
    @Test
    void testRoundTrip() {
        // 测试完整流程：Oracle门名称 → 合并 → 提取 → 比较
        List<String> oracleNames = Arrays.asList("化工西门", "化肥西门", "复合肥南门");
        
        // 生成VIP类型名称
        String vipTypeName = VehiclePermissionMerger.generateVipTypeNameFromOracleNames(oracleNames);
        assertEquals("化工西化肥西复合肥南VIP", vipTypeName);
        
        // 从VIP类型名称提取权限
        Set<String> extractedPermissions = VehiclePermissionComparator.extractPermissionsFromVipTypeName(vipTypeName);
        assertEquals(3, extractedPermissions.size());
        
        // 比较权限（应该相同）
        assertTrue(VehiclePermissionComparator.compareOracleAndAkePermissions(
                oracleNames, vipTypeName, false));
    }
}
