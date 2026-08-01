package com.novel.common.aspect;

import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
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

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(com.novel.common.annotation.AuthToken)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文，拒绝访问: {}", joinPoint.getSignature().getName());
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        HttpServletRequest request = attributes.getRequest();

        // 优先从网关传递的 Header 获取身份信息
        String userIdStr = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");

        Long userId = null;

        if (!isBlank(userIdStr)) {
            try {
                userId = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException e) {
                log.error("解析 X-User-Id 失败: {}", userIdStr);
            }
        } else {
            // 网关未传递时，尝试从 Authorization Token 解析（兜底逻辑）
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.getClaimsFromToken(token);
                if (claims != null && jwtUtil.validateToken(token)) {
                    userId = jwtUtil.getUserIdFromToken(token);
                    username = jwtUtil.getUsernameFromToken(token);
                }
            }
        }

        // 未解析出有效用户身份则拒绝放行，避免无校验直接 proceed
        if (userId == null) {
            log.warn("未获取到有效用户身份，拒绝访问: {}", request.getRequestURI());
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 仅存入非空值，避免污染上下文
        UserContext.setUserId(userId);
        if (username != null && !username.trim().isEmpty()) {
            UserContext.setUsername(username.trim());
        } else {
            UserContext.setUsername("");
        }

        try {
            return joinPoint.proceed();
        } finally {
            UserContext.clear();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
