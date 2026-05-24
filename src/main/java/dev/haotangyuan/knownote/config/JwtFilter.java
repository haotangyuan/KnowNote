package dev.haotangyuan.knownote.config;

import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.JwtUtil;
import dev.haotangyuan.knownote.common.Result;
import dev.haotangyuan.knownote.common.UserContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/**",
            "/actuator/**",
            "/error"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            if (isPublicPath(req.getRequestURI())) {
                chain.doFilter(request, response);
                return;
            }

            String token = extractToken(req);
            if (!StringUtils.hasText(token)) {
                writeError(res, "未登录");
                return;
            }

            // 验证 token 有效性且必须是 access token
            if (!jwtUtil.isValid(token) || !jwtUtil.isAccessToken(token)) {
                writeError(res, "登录已过期，请重新登录");
                return;
            }

            UserContext.set(jwtUtil.getUserId(token), null);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isPublicPath(String uri) {
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, uri));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(ErrorCode.CLIENT_ERROR, message)
        ));
    }
}
