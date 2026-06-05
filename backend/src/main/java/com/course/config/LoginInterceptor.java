package com.course.config;

import com.course.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("[拦截器] " + request.getMethod() + " " + request.getRequestURI());

        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("[拦截器] OPTIONS 放行");
            return true;
        }

        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            System.out.println("[拦截器] 无 token → 401");
            response.setStatus(401);
            return false;
        }
        try {
            var claims = JwtUtil.parseToken(token);
            // 把 userId 和 username 存入 request，Controller 可以直接取
            request.setAttribute("userId", claims.get("id", Long.class));
            request.setAttribute("username", claims.getSubject());
            System.out.println("[拦截器] token 有效, userId=" + claims.get("id") + " → 放行");
            return true;
        } catch (Exception e) {
            System.out.println("[拦截器] token 无效 → 401: " + e.getMessage());
            response.setStatus(401);
            return false;
        }
    }
}
