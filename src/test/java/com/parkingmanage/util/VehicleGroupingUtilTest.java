package com.parkingmanage.util;

import com.parkingmanage.entity.GroupedVehicleInfo;
import com.parkingmanage.entity.OracleVehicleInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆数据分组工具类测试
 */
class VehicleGroupingUtilTest {

    /**
     * 测试单车牌单记录的情况
     */
    @Test
    void testGroupByPlateNumber_SingleRecord() {
        // 准备测试数据
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOwnerName("张三");
        vehicle1.setOrgNo("001");
        vehicle1.setOrgName("化工西门");
        vehicle1.setValidStartTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        vehicle1.setValidEndTime(LocalDateTime.of(2025, 12, 31, 23, 59));
        vehicles.add(vehicle1);
        
        // 执行分组
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        // 验证结果
        assertEquals(1, result.size(), "应该有1个分组");
        
        GroupedVehicleInfo grouped = result.get(0);
        assertEquals("黑A12345", grouped.getPlateNumber());
        assertEquals("张三", grouped.getOwnerName());
        assertEquals(1, grouped.getRecordCount(), "应该有1条记录");
        assertEquals(1, grouped.getOrgNames().size(), "应该有1个厂区");
        assertEquals("化工西门", grouped.getOrgNames().get(0));
    }

    /**
     * 测试单车牌多记录的情况（核心场景）
     */
    @Test
    void testGroupByPlateNumber_MultipleRecords() {
        // 准备测试数据：同一车牌有3条记录（3个不同厂区）
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        // 记录1：化工西门
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOwnerName("张三");
        vehicle1.setOrgNo("001");
        vehicle1.setOrgName("化工西门");
        vehicle1.setValidStartTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        vehicle1.setValidEndTime(LocalDateTime.of(2025, 12, 31, 23, 59));
        vehicles.add(vehicle1);
        
        // 记录2：化肥西门（同一车牌）
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber("黑A12345");
        vehicle2.setOwnerName("张三");
        vehicle2.setOrgNo("002");
        vehicle2.setOrgName("化肥西门");
        vehicle2.setValidStartTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        vehicle2.setValidEndTime(LocalDateTime.of(2025, 12, 31, 23, 59));
        vehicles.add(vehicle2);
        
        // 记录3：复合肥南门（同一车牌）
        OracleVehicleInfo vehicle3 = new OracleVehicleInfo();
        vehicle3.setPlateNumber("黑A12345");
        vehicle3.setOwnerName("张三");
        vehicle3.setOrgNo("003");
        vehicle3.setOrgName("复合肥南门");
        vehicle3.setValidStartTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        vehicle3.setValidEndTime(LocalDateTime.of(2025, 12, 31, 23, 59));
        vehicles.add(vehicle3);
        
        // 执行分组
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        // 验证结果
        assertEquals(1, result.size(), "应该有1个分组（同一车牌）");
        
        GroupedVehicleInfo grouped = result.get(0);
        assertEquals("黑A12345", grouped.getPlateNumber());
        assertEquals("张三", grouped.getOwnerName());
        assertEquals(3, grouped.getRecordCount(), "应该有3条记录");
        assertEquals(3, grouped.getOrgNames().size(), "应该有3个厂区");
        
        // 验证厂区名称列表
        assertTrue(grouped.getOrgNames().contains("化工西门"));
        assertTrue(grouped.getOrgNames().contains("化肥西门"));
        assertTrue(grouped.getOrgNames().contains("复合肥南门"));
        
        // 验证基本信息取自第一条记录
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), grouped.getValidStartTime());
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59), grouped.getValidEndTime());
    }

    /**
     * 测试多车牌混合的情况
     */
    @Test
    void testGroupByPlateNumber_MultiplePlates() {
        // 准备测试数据：2个车牌，第一个有2条记录，第二个有1条记录
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        // 车牌1 - 记录1
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOwnerName("张三");
        vehicle1.setOrgNo("001");
        vehicle1.setOrgName("化工西门");
        vehicles.add(vehicle1);
        
        // 车牌1 - 记录2
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber("黑A12345");
        vehicle2.setOwnerName("张三");
        vehicle2.setOrgNo("002");
        vehicle2.setOrgName("化肥西门");
        vehicles.add(vehicle2);
        
        // 车牌2 - 记录1
        OracleVehicleInfo vehicle3 = new OracleVehicleInfo();
        vehicle3.setPlateNumber("黑B67890");
        vehicle3.setOwnerName("李四");
        vehicle3.setOrgNo("001");
        vehicle3.setOrgName("化工西门");
        vehicles.add(vehicle3);
        
        // 执行分组
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        // 验证结果
        assertEquals(2, result.size(), "应该有2个分组（2个不同车牌）");
        
        // 验证第一个分组（黑A12345）
        GroupedVehicleInfo grouped1 = result.get(0);
        assertEquals("黑A12345", grouped1.getPlateNumber());
        assertEquals(2, grouped1.getRecordCount(), "车牌1应该有2条记录");
        assertEquals(2, grouped1.getOrgNames().size(), "车牌1应该有2个厂区");
        
        // 验证第二个分组（黑B67890）
        GroupedVehicleInfo grouped2 = result.get(1);
        assertEquals("黑B67890", grouped2.getPlateNumber());
        assertEquals(1, grouped2.getRecordCount(), "车牌2应该有1条记录");
        assertEquals(1, grouped2.getOrgNames().size(), "车牌2应该有1个厂区");
    }

    /**
     * 测试空列表的情况
     */
    @Test
    void testGroupByPlateNumber_EmptyList() {
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        assertEquals(0, result.size(), "空列表应该返回空结果");
    }

    /**
     * 测试null输入的情况
     */
    @Test
    void testGroupByPlateNumber_NullInput() {
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(null);
        
        assertNotNull(result, "null输入应该返回非null的空列表");
        assertEquals(0, result.size(), "null输入应该返回空结果");
    }

    /**
     * 测试车牌号为空的记录会被跳过
     */
    @Test
    void testGroupByPlateNumber_SkipNullPlateNumber() {
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        // 正常记录
        OracleVehicleInfo vehicle1 = new OracleVehicleInfo();
        vehicle1.setPlateNumber("黑A12345");
        vehicle1.setOwnerName("张三");
        vehicle1.setOrgNo("001");
        vehicle1.setOrgName("化工西门");
        vehicles.add(vehicle1);
        
        // 车牌号为null的记录
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber(null);
        vehicle2.setOwnerName("李四");
        vehicle2.setOrgNo("002");
        vehicle2.setOrgName("化肥西门");
        vehicles.add(vehicle2);
        
        // 车牌号为空字符串的记录
        OracleVehicleInfo vehicle3 = new OracleVehicleInfo();
        vehicle3.setPlateNumber("  ");
        vehicle3.setOwnerName("王五");
        vehicle3.setOrgNo("003");
        vehicle3.setOrgName("复合肥南门");
        vehicles.add(vehicle3);
        
        // 执行分组
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        // 验证结果：只有正常记录被分组
        assertEquals(1, result.size(), "应该只有1个分组（跳过无效车牌号）");
        assertEquals("黑A12345", result.get(0).getPlateNumber());
    }

    /**
     * 测试分组统计信息
     */
    @Test
    void testGetGroupingStatistics() {
        List<OracleVehicleInfo> vehicles = new ArrayList<>();
        
        // 车牌1：3条记录
        for (int i = 0; i < 3; i++) {
            OracleVehicleInfo vehicle = new OracleVehicleInfo();
            vehicle.setPlateNumber("黑A12345");
            vehicle.setOwnerName("张三");
            vehicle.setOrgNo("00" + (i + 1));
            vehicle.setOrgName("厂区" + (i + 1));
            vehicles.add(vehicle);
        }
        
        // 车牌2：1条记录
        OracleVehicleInfo vehicle2 = new OracleVehicleInfo();
        vehicle2.setPlateNumber("黑B67890");
        vehicle2.setOwnerName("李四");
        vehicle2.setOrgNo("001");
        vehicle2.setOrgName("化工西门");
        vehicles.add(vehicle2);
        
        // 执行分组
        List<GroupedVehicleInfo> result = VehicleGroupingUtil.groupByPlateNumber(vehicles);
        
        // 获取统计信息
        String statistics = VehicleGroupingUtil.getGroupingStatistics(result);
        
        // 验证统计信息包含关键数据
        assertNotNull(statistics);
        assertTrue(statistics.contains("车辆数=2"), "应该包含车辆数");
        assertTrue(statistics.contains("总记录数=4"), "应该包含总记录数");
        assertTrue(statistics.contains("最多记录数=3"), "应该包含最多记录数");
        assertTrue(statistics.contains("黑A12345"), "应该包含最多记录的车牌号");
    }
}
