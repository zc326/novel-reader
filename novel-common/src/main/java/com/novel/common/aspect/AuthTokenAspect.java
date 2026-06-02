package com.novel.common.aspect;

import com.novel.common.utils.JwtUtil;
import com.novel.common.utils.UserContext;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Configuration
public class AuthTokenAspect {

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    @Around("@annotation(com.novel.common.annotation.AuthToken)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("=== AuthTokenAspect 被触发 ===");
        log.info("目标方法: {}", joinPoint.getSignature().getName());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 打印所有Header，用于调试
        log.info("=== 请求头列表 ===");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("{}: {}", headerName, request.getHeader(headerName));
        }
        log.info("==================");
        
        // 优先从 X-User-Id Header 获取（网关传递）
        String userIdStr = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");

        log.info("请求路径: {}", request.getRequestURI());
        log.info("X-User-Id header: {}", userIdStr);
        log.info("X-Username header: {}", username);

        Long userId = null;
        
        // 如果网关没有传递Header，尝试从 Authorization Token 中解析
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("从Authorization Header获取Token，尝试解析");
                
                if (jwtUtil != null) {
                    try {
                        Claims claims = jwtUtil.getClaimsFromToken(token);
                        if (claims != null) {
                            userId = jwtUtil.getUserIdFromToken(token);
                            username = jwtUtil.getUsernameFromToken(token);
                            log.info("Token解析成功 - userId: {}, username: {}", userId, username);
                        } else {
                            log.warn("Token解析失败，claims为null");
                        }
                    } catch (Exception e) {
                        log.error("Token解析异常: {}", e.getMessage());
                    }
                } else {
                    log.warn("JwtUtil未注入，无法解析Token");
                }
            }
        } else {
            // 从网关传递的Header中解析
            try {
                userId = Long.parseLong(userIdStr.trim());
                log.info("解析userId成功: {}", userId);
            } catch (NumberFormatException e) {
                log.error("解析X-User-Id失败: {}", userIdStr);
            }
        }

        UserContext.setUserId(userId);
        UserContext.setUsername(username);

        log.info("UserContext设置完成 - userId: {}, username: {}", userId, username);

        try {
            Object result = joinPoint.proceed();
            log.info("方法执行成功");
            return result;
        } catch (Exception e) {
            log.error("方法执行异常", e);
            throw e;
        } finally {
            UserContext.clear();
            log.info("=== AuthTokenAspect 结束 ===");
        }
    }
}