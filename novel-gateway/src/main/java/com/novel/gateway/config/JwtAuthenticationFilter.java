package com.novel.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:novel-reader-secret-key-2024}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.equals("/user/login") || path.equals("/user/register")) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/user/") || path.startsWith("/book/") || path.startsWith("/bookshelf/")) {
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                log.warn("请求路径: {}, 缺少Token", path);
                return unauthorized(exchange.getResponse(), "缺少认证令牌");
            }

            try {
                if (!validateToken(token)) {
                    log.warn("请求路径: {}, Token验证失败", path);
                    return unauthorized(exchange.getResponse(), "Token无效或已过期");
                }

                String userId = getUserIdFromToken(token);
                String username = getUsernameFromToken(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                log.error("Token解析异常: {}", e.getMessage());
                return unauthorized(exchange.getResponse(), "Token解析异常");
            }
        }

        return chain.filter(exchange);
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.warn("Token已过期");
                return false;
            }
            
            Object userId = claims.get("userId");
            Object username = claims.get("username");
            
            if (userId == null || username == null) {
                log.warn("Token缺少必要字段");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    private String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return String.valueOf(((Integer) userId).longValue());
            } else if (userId instanceof Long) {
                return String.valueOf(userId);
            }
            return userId.toString();
        } catch (Exception e) {
            log.error("解析userId失败: {}", e.getMessage());
            return "0";
        }
    }

    private String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (Exception e) {
            log.error("解析username失败: {}", e.getMessage());
            return "unknown";
        }
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}