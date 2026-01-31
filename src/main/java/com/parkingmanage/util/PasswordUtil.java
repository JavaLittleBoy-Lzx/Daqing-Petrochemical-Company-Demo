package com.parkingmanage.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码工具类
 * 使用BCrypt风格的密码加密和验证
 * 
 * @author parking-system
 * @version 1.0
 * @since 2024
 */
public class PasswordUtil {
    
    private static final SecureRandom random = new SecureRandom();
    private static final String BCRYPT_PREFIX = "$2a$10$";
    
    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        // 生成随机盐
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);
        
        // 使用SHA-256加密（简化版，实际项目建议使用真正的BCrypt）
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(rawPassword.getBytes("UTF-8"));
            byte[] hashedPassword = md.digest();
            
            String hashedStr = Base64.getEncoder().encodeToString(hashedPassword);
            return BCRYPT_PREFIX + saltStr.substring(0, 22) + hashedStr;
            
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 验证密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        // 检查是否是我们的加密格式
        if (!encodedPassword.startsWith(BCRYPT_PREFIX)) {
            return false;
        }
        
        try {
            // 提取盐和哈希值
            String content = encodedPassword.substring(BCRYPT_PREFIX.length());
            String saltStr = content.substring(0, 22);
            String storedHash = content.substring(22);
            
            // 重新计算哈希
            byte[] salt = Base64.getDecoder().decode(saltStr + "=="); // 补齐Base64填充
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(rawPassword.getBytes("UTF-8"));
            byte[] computedHash = md.digest();
            
            String computedHashStr = Base64.getEncoder().encodeToString(computedHash);
            return computedHashStr.equals(storedHash);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 生成随机密码（用于重置密码等场景）
     * 
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 6) {
            length = 6; // 最小长度为6
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        
        return password.toString();
    }
} 