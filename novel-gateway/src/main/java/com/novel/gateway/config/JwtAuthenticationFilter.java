package com.novel.gateway.config;

import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:novel-reader-secret-key-2024}")
    private String secret;

    /**
     * 配置化白名单：无需认证的路径前缀集合。
     * 覆盖登录、注册等开放接口，避免硬编码且可随业务扩展。
     */
    private static final Set<String> WHITE_LIST_PREFIXES = new HashSet<>();

    static {
        WHITE_LIST_PREFIXES.add("/user/login");
        WHITE_LIST_PREFIXES.add("/user/register");
    }

    /**
     * 需要认证校验的路径前缀集合。
     */
    private static final Set<String> PROTECTED_PREFIXES = new HashSet<>();

    static {
        PROTECTED_PREFIXES.add("/user/");
        PROTECTED_PREFIXES.add("/book/");
        PROTECTED_PREFIXES.add("/bookshelf/");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单直接放行（支持前缀匹配，覆盖带前后缀的路径）
        if (isInWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 2. 非受保护路径直接放行
        if (!isProtected(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求路径: {}, 缺少Token", path);
            return unauthorized(exchange.getResponse(), ResultCode.UNAUTHORIZED.getMessage());
        }

        try {
            // 3. 只解析一次 Token，缓存 Claims 并复用
            Claims claims = parseClaims(token);
            if (claims == null) {
                log.warn("请求路径: {}, Token解析失败", path);
                return unauthorized(exchange.getResponse(), "Token无效或已过期");
            }

            if (isExpired(claims)) {
                log.warn("请求路径: {}, Token已过期", path);
                return unauthorized(exchange.getResponse(), "Token已过期");
            }

            Object userIdObj = claims.get("userId");
            Object usernameObj = claims.get("username");
            if (userIdObj == null || usernameObj == null) {
                log.warn("请求路径: {}, Token缺少必要字段", path);
                return unauthorized(exchange.getResponse(), "Token缺少必要字段");
            }

            String userId = resolveUserId(userIdObj);
            String username = String.valueOf(usernameObj);

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

    private boolean isInWhiteList(String path) {
        for (String prefix : WHITE_LIST_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtected(String path) {
        for (String prefix : PROTECTED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 解析 Token，验证签名并提取 Claims。整个流程只解析一次。
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean isExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    private String resolveUserId(Object userIdObj) {
        if (userIdObj instanceof Integer) {
            return String.valueOf(((Integer) userIdObj).longValue());
        } else if (userIdObj instanceof Long) {
            return String.valueOf(userIdObj);
        }
        return String.valueOf(userIdObj);
    }

    /**
     * 使用统一 Result 格式返回 401，避免手动拼接 JSON 导致前端解析问题。
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = Result.error(ResultCode.UNAUTHORIZED.getCode(), message).toString();
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
