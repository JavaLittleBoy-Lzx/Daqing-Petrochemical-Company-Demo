package com.parkingmanage.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VIP权限工具类测试
 */
public class VipPermissionUtilTest {
    
    @Test
    public void testExtractPermissionsFromVipType() {
        // 测试单门权限
        Set<String> permissions1 = VipPermissionUtil.extractPermissionsFromVipType("化工西VIP");
        assertEquals(1, permissions1.size());
        assertTrue(permissions1.contains("化工西"));
        
        // 测试双门权限
        Set<String> permissions2 = VipPermissionUtil.extractPermissionsFromVipType("化工西化肥西VIP");
        assertEquals(2, permissions2.size());
        assertTrue(permissions2.contains("化工西"));
        assertTrue(permissions2.contains("化肥西"));
        
        // 测试三门权限
        Set<String> permissions3 = VipPermissionUtil.extractPermissionsFromVipType("化工西化肥西复合肥南VIP");
        assertEquals(3, permissions3.size());
        assertTrue(permissions3.contains("化工西"));
        assertTrue(permissions3.contains("化肥西"));
        assertTrue(permissions3.contains("复合肥南"));
        
        // 测试空字符串
        Set<String> permissions4 = VipPermissionUtil.extractPermissionsFromVipType("");
        assertTrue(permissions4.isEmpty());
        
        // 测试null
        Set<String> permissions5 = VipPermissionUtil.extractPermissionsFromVipType(null);
        assertTrue(permissions5.isEmpty());
    }
    
    @Test
    public void testExtractPermissionsFromBlacklistType() {
        // 测试单门权限
        Set<String> permissions1 = VipPermissionUtil.extractPermissionsFromBlacklistType("请停车检查（化工西）");
        assertEquals(1, permissions1.size());
        assertTrue(permissions1.contains("化工西"));
        
        // 测试双门权限
        Set<String> permissions2 = VipPermissionUtil.extractPermissionsFromBlacklistType("请停车检查（化工西化肥西）");
        assertEquals(2, permissions2.size());
        assertTrue(permissions2.contains("化工西"));
        assertTrue(permissions2.contains("化肥西"));
        
        // 测试三门权限
        Set<String> permissions3 = VipPermissionUtil.extractPermissionsFromBlacklistType("请停车检查（化工西化肥西复合肥南）");
        assertEquals(3, permissions3.size());
        assertTrue(permissions3.contains("化工西"));
        assertTrue(permissions3.contains("化肥西"));
        assertTrue(permissions3.contains("复合肥南"));
    }
    
    @Test
    public void testArePermissionsEqual() {
        Set<String> permissions1 = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西"));
        Set<String> permissions2 = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西"));
        Set<String> permissions3 = new LinkedHashSet<>(Arrays.asList("化肥西", "化工西")); // 顺序不同
        Set<String> permissions4 = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西", "复合肥南"));
        
        // 相同权限
        assertTrue(VipPermissionUtil.arePermissionsEqual(permissions1, permissions2));
        
        // 顺序不同但内容相同
        assertTrue(VipPermissionUtil.arePermissionsEqual(permissions1, permissions3));
        
        // 不同权限
        assertFalse(VipPermissionUtil.arePermissionsEqual(permissions1, permissions4));
        
        // null处理
        assertTrue(VipPermissionUtil.arePermissionsEqual(null, null));
        assertFalse(VipPermissionUtil.arePermissionsEqual(permissions1, null));
        assertFalse(VipPermissionUtil.arePermissionsEqual(null, permissions1));
    }
    
    @Test
    public void testExtractPermissionsFromOracleGateNames() {
        List<String> oracleNames = Arrays.asList("化工西门", "化肥西门", "复合肥南门");
        Set<String> permissions = VipPermissionUtil.extractPermissionsFromOracleGateNames(oracleNames);
        
        assertEquals(3, permissions.size());
        assertTrue(permissions.contains("化工西"));
        assertTrue(permissions.contains("化肥西"));
        assertTrue(permissions.contains("复合肥南"));
    }
    
    @Test
    public void testMergePermissionsToVipType() {
        Set<String> permissions = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西", "复合肥南"));
        String vipTypeName = VipPermissionUtil.mergePermissionsToVipType(permissions);
        
        assertEquals("化工西化肥西复合肥南VIP", vipTypeName);
    }
    
    @Test
    public void testMergePermissionsToBlacklistType() {
        Set<String> permissions = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西", "复合肥南"));
        String blacklistTypeName = VipPermissionUtil.mergePermissionsToBlacklistType(permissions);
        
        assertEquals("请停车检查（化工西化肥西复合肥南）", blacklistTypeName);
    }
    
    @Test
    public void testRoundTrip() {
        // 测试VIP类型名称的往返转换
        Set<String> originalPermissions = new LinkedHashSet<>(Arrays.asList("化工西", "化肥西"));
        String vipTypeName = VipPermissionUtil.mergePermissionsToVipType(originalPermissions);
        Set<String> extractedPermissions = VipPermissionUtil.extractPermissionsFromVipType(vipTypeName);
        
        assertTrue(VipPermissionUtil.arePermissionsEqual(originalPermissions, extractedPermissions));
        
        // 测试黑名单类型名称的往返转换
        String blacklistTypeName = VipPermissionUtil.mergePermissionsToBlacklistType(originalPermissions);
        Set<String> extractedBlacklistPermissions = VipPermissionUtil.extractPermissionsFromBlacklistType(blacklistTypeName);
        
        assertTrue(VipPermissionUtil.arePermissionsEqual(originalPermissions, extractedBlacklistPermissions));
    }
}
