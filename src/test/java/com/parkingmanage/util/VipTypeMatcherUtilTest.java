package com.parkingmanage.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VIP类型智能匹配工具类测试
 */
public class VipTypeMatcherUtilTest {
    
    @Test
    public void testFindBestMatchVipType_ExactMatch() {
        // 测试完全匹配
        String oracleVipType = "化工西化肥西VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipType(oracleVipType);
        assertEquals("化工西化肥西VIP", matched);
    }
    
    @Test
    public void testFindBestMatchVipType_PartialMatch() {
        // 测试部分匹配 - Oracle有4个权限，AKE最多只有3个
        String oracleVipType = "化工西化肥西复合肥南炼油东VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipType(oracleVipType);
        
        // 应该匹配到包含最多权限的类型
        assertNotNull(matched);
        assertTrue(matched.equals("化工西化肥西复合肥南VIP") || 
                   matched.equals("化工西化肥西炼油东VIP") ||
                   matched.equals("化工西复合肥南炼油东VIP") ||
                   matched.equals("化肥西复合肥南炼油东VIP"));
    }
    
    @Test
    public void testFindBestMatchVipType_SingleGate() {
        // 测试单门匹配
        String oracleVipType = "化工西VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipType(oracleVipType);
        assertEquals("化工西VIP", matched);
    }
    
    @Test
    public void testFindBestMatchVipType_NoMatch() {
        // 测试无法匹配的情况
        String oracleVipType = "未知门VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipType(oracleVipType);
        assertNull(matched);
    }
    
    @Test
    public void testFindBestMatchVipType_EmptyInput() {
        // 测试空输入
        String matched = VipTypeMatcherUtil.findBestMatchVipType("");
        assertNull(matched);
        
        matched = VipTypeMatcherUtil.findBestMatchVipType(null);
        assertNull(matched);
    }
    
    @Test
    public void testFindBestMatchVipTypeWithFallback() {
        // 测试带回退策略的匹配
        String oracleVipType = "未知门VIP";
        String defaultVipType = "化工西VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipTypeWithFallback(oracleVipType, defaultVipType);
        assertEquals(defaultVipType, matched);
    }
    
    @Test
    public void testCalculateSimilarity_Identical() {
        // 测试相同VIP类型的相似度
        String vipType1 = "化工西化肥西VIP";
        String vipType2 = "化工西化肥西VIP";
        double similarity = VipTypeMatcherUtil.calculateSimilarity(vipType1, vipType2);
        assertEquals(1.0, similarity, 0.001);
    }
    
    @Test
    public void testCalculateSimilarity_Partial() {
        // 测试部分相似的VIP类型
        String vipType1 = "化工西化肥西VIP";
        String vipType2 = "化工西复合肥南VIP";
        double similarity = VipTypeMatcherUtil.calculateSimilarity(vipType1, vipType2);
        
        // 交集: {化工西}, 并集: {化工西, 化肥西, 复合肥南}
        // 相似度 = 1/3 ≈ 0.333
        assertTrue(similarity > 0.3 && similarity < 0.4);
    }
    
    @Test
    public void testCalculateSimilarity_NoOverlap() {
        // 测试完全不同的VIP类型
        String vipType1 = "化工西VIP";
        String vipType2 = "化肥西VIP";
        double similarity = VipTypeMatcherUtil.calculateSimilarity(vipType1, vipType2);
        
        // 交集: {}, 并集: {化工西, 化肥西}
        // 相似度 = 0/2 = 0
        assertEquals(0.0, similarity, 0.001);
    }
    
    @Test
    public void testRealWorldScenario() {
        // 测试真实场景：Oracle中的复杂VIP类型
        String oracleVipType = "化工西化肥西复合肥南炼油东VIP";
        String matched = VipTypeMatcherUtil.findBestMatchVipType(oracleVipType);
        
        assertNotNull(matched, "应该能找到匹配的VIP类型");
        
        // 验证匹配的类型包含至少3个权限
        int matchedPermissionCount = VipPermissionUtil.extractPermissionsFromVipType(matched).size();
        assertTrue(matchedPermissionCount >= 3, "匹配的VIP类型应该包含至少3个权限");
    }
}
