package com.novel.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret:novel-reader-secret-key-2024}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }

    /**
     * 完整校验 Token：
     * 1. 签名/格式有效（parseClaimsJws 已校验签名算法与签名值）
     * 2. 未过期
     * 3. 必要字段 userId、username 均存在且非空
     */
    public boolean validateToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        if (isTokenExpired(token)) {
            return false;
        }
        Object userId = claims.get("userId");
        Object username = claims.get("username");
        return userId != null && username != null && !username.toString().trim().isEmpty();
    }

    /**
     * 解析 Token 并捕获所有 JWT 相关异常，便于上层区分错误类型。
     */
    public Claims parseClaimsJwsToken(String token) throws ExpiredJwtException, UnsupportedJwtException,
            MalformedJwtException, SignatureException, IllegalArgumentException {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}
