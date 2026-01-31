package com.parkingmanage.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成、验证和解析JWT令牌
 * 使用 auth0 java-jwt 库
 * 
 * @author parking-system
 * @version 1.0
 */
@Component
public class JwtUtil {

    // JWT密钥，实际使用时应该从配置文件读取
    @Value("${jwt.secret:parkingSystemSecretKeyForJwtTokenGeneration2024}")
    private String secret;

    // JWT过期时间（24小时）
    @Value("${jwt.expiration:86400}")
    private Long expiration;

    // 获取签名算法
    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }

    /**
     * 生成JWT令牌
     * 
     * @param claims 载荷信息
     * @return JWT令牌
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withIssuedAt(now)
                .withExpiresAt(expiryDate);

        // 添加自定义claims
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                builder.withClaim(key, (String) value);
            } else if (value instanceof Integer) {
                builder.withClaim(key, (Integer) value);
            } else if (value instanceof Long) {
                builder.withClaim(key, (Long) value);
            } else if (value instanceof Boolean) {
                builder.withClaim(key, (Boolean) value);
            } else {
                builder.withClaim(key, value.toString());
            }
        }

        return builder.sign(getAlgorithm());
    }

    /**
     * 从令牌中获取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("username").asString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("userId").asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取车场名称
     * 
     * @param token JWT令牌
     * @return 车场名称
     */
    public String getParkNameFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("parkName").asString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从令牌中获取职位
     * 
     * @param token JWT令牌
     * @return 职位
     */
    public String getPositionFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("position").asString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取令牌的过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getExpiresAt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查令牌是否过期
     * 
     * @param token JWT令牌
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证令牌
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    public Boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            verifier.verify(token);
            return !isTokenExpired(token);
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 解码令牌
     * 
     * @param token JWT令牌
     * @return DecodedJWT对象
     */
    private DecodedJWT decodeToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /**
     * 刷新令牌
     * 
     * @param token 原令牌
     * @return 新令牌
     */
    public String refreshToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            if (decodedJWT != null) {
                // 提取原有的claims并生成新令牌
                Map<String, Object> claims = new java.util.HashMap<>();
                claims.put("userId", decodedJWT.getClaim("userId").asInt());
                claims.put("username", decodedJWT.getClaim("username").asString());
                claims.put("parkName", decodedJWT.getClaim("parkName").asString());
                claims.put("position", decodedJWT.getClaim("position").asString());
                
                return generateToken(claims);
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }

    /**
     * 检查令牌是否可以刷新
     * 
     * @param token JWT令牌
     * @return 是否可以刷新
     */
    public Boolean canTokenBeRefreshed(String token) {
        return !isTokenExpired(token) || ignoreTokenExpiration(token);
    }

    /**
     * 忽略令牌过期（在刷新时使用）
     * 
     * @param token JWT令牌
     * @return 是否忽略过期
     */
    private Boolean ignoreTokenExpiration(String token) {
        // 可以在这里添加特殊逻辑，比如在令牌过期后的一定时间内仍然允许刷新
        return false;
    }
} 