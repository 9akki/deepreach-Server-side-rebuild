package com.deepreach.common.security;

import com.deepreach.common.core.domain.model.LoginUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌工具类
 *
 * 设计理念：
 * 1. 架构无关 - 支持所有阶段的令牌管理
 * 2. 安全可靠 - 使用HS512算法，支持密钥轮换
 * 3. 功能完整 - 支持访问令牌和刷新令牌
 * 4. 易于扩展 - 支持自定义Claims
 *
 * @author DeepReach Team
 * @version 1.0
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:deepreach-default-secret-key-change-in-production}")
    private String secret;

    @Value("${jwt.expiration:86400}")  // 默认24小时
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")  // 默认7天
    private Long refreshExpiration;

    @Value("${jwt.issuer:deepreach}")
    private String issuer;

    @Value("${jwt.audience:deepreach-users}")
    private String audience;

    // 令牌前缀
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TYPE_CLAIM = "type";
    private static final String USER_ID_CLAIM = "userId";
    private static final String DEPT_ID_CLAIM = "deptId";
    private static final String EMAIL_CLAIM = "email";

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object userId = claims.get(USER_ID_CLAIM);
            return userId != null ? Long.valueOf(userId.toString()) : null;
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取部门ID
     */
    public Long getDeptIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object deptId = claims.get(DEPT_ID_CLAIM);
            return deptId != null ? Long.valueOf(deptId.toString()) : null;
        } catch (Exception e) {
            log.error("Failed to extract dept ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取邮箱
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return (String) claims.get(EMAIL_CLAIM);
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取签发时间
     */
    public Date getIssuedDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    /**
     * 从token中获取指定claim
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有claims
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("JWT token signature is invalid: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT token is invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 检查token是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Failed to check token expiration: {}", e.getMessage());
            return true; // 出错时认为token已过期
        }
    }

    /**
     * 为LoginUser生成访问令牌
     */
    public String generateToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();

        // 添加用户基本信息
        claims.put(USER_ID_CLAIM, loginUser.getUserId());
        claims.put(DEPT_ID_CLAIM, loginUser.getDeptId());
        claims.put(EMAIL_CLAIM, loginUser.getEmail());
        claims.put(TYPE_CLAIM, "access");

        return createToken(claims, loginUser.getUsername(), expiration);
    }

    /**
     * 为LoginUser生成刷新令牌
     */
    public String generateRefreshToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();

        claims.put(USER_ID_CLAIM, loginUser.getUserId());
        claims.put(TYPE_CLAIM, "refresh");

        return createToken(claims, loginUser.getUsername(), refreshExpiration);
    }

    /**
     * 生成简单的令牌（仅用户名）
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TYPE_CLAIM, "access");
        return createToken(claims, username, expiration);
    }

    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime * 1000);

        try {
            return Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuer(issuer)
                    .audience().add(audience).and()
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            log.error("Failed to create JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create token", e);
        }
    }

    /**
     * 验证token（用户名匹配）
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证token（仅检查有效性和过期时间）
     */
    public Boolean validateToken(String token) {
        try {
            // 尝试解析token，如果成功则说明格式和签名正确
            Claims claims = getAllClaimsFromToken(token);

            // 检查是否过期
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired");
            return false;
        } catch (Exception e) {
            log.debug("JWT token is invalid: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为刷新令牌
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "refresh".equals(claims.get(TYPE_CLAIM));
        } catch (Exception e) {
            log.debug("Failed to check if token is refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否为访问令牌
     */
    public Boolean isAccessToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "access".equals(claims.get(TYPE_CLAIM));
        } catch (Exception e) {
            log.debug("Failed to check if token is access token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求头中提取token
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 获取token剩余有效时间（秒）
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            log.debug("Failed to get token remaining time: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 检查token是否需要刷新（距离过期时间小于1小时）
     */
    public Boolean shouldRefreshToken(String token) {
        Long remainingTime = getTokenRemainingTime(token);
        return remainingTime < 3600; // 1小时
    }

    /**
     * 解析token获取所有信息
     */
    public TokenInfo parseToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);

            return TokenInfo.builder()
                    .subject(claims.getSubject())
                    .userId(getUserIdFromToken(token))
                    .deptId(getDeptIdFromToken(token))
                    .email(getEmailFromToken(token))
                    .issuedAt(claims.getIssuedAt())
                    .expiration(claims.getExpiration())
                    .issuer(claims.getIssuer())
                    .audience(claims.getAudience() != null && !claims.getAudience().isEmpty() ?
                     claims.getAudience().iterator().next() : null)
                    .type((String) claims.get(TYPE_CLAIM))
                    .isExpired(isTokenExpired(token))
                    .remainingTime(getTokenRemainingTime(token))
                    .shouldRefresh(shouldRefreshToken(token))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse token", e);
        }
    }

    /**
     * 刷新令牌
     */
    public String refreshToken(String oldToken, LoginUser loginUser) {
        try {
            // 验证旧token
            if (!validateToken(oldToken) || !isRefreshToken(oldToken)) {
                throw new IllegalArgumentException("Invalid refresh token");
            }

            // 生成新的访问令牌
            return generateToken(loginUser);
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    /**
     * 检查密钥强度
     */
    public boolean isSecretStrong() {
        return secret != null && secret.length() >= 32;
    }

    /**
     * 获取令牌信息传输对象
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenInfo {
        private String subject;
        private Long userId;
        private Long deptId;
        private String email;
        private Date issuedAt;
        private Date expiration;
        private String issuer;
        private String audience;
        private String type;
        private Boolean isExpired;
        private Long remainingTime;
        private Boolean shouldRefresh;
    }
}