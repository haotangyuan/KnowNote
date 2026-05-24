package dev.haotangyuan.knownote.user.service;

import dev.haotangyuan.knownote.common.BizException;
import dev.haotangyuan.knownote.common.ErrorCode;
import dev.haotangyuan.knownote.common.JwtUtil;
import dev.haotangyuan.knownote.user.api.dto.resp.TokenRespDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 签发 Token 对
     */
    public TokenRespDTO issueTokenPair(Long userId, String deviceInfo) {
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        // 存储 refresh token 到 Redis
        storeRefreshToken(userId, refreshToken, deviceInfo);

        return new TokenRespDTO(accessToken, refreshToken);
    }

    /**
     * 刷新 Token
     */
    public TokenRespDTO refresh(String refreshToken) {
        // 验证 refresh token
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "登录已过期，请重新登录");
        }

        Long userId = jwtUtil.getUserId(refreshToken);

        // 检查 Redis 中是否存在
        String key = REFRESH_TOKEN_PREFIX + userId;
        Boolean exists = redisTemplate.opsForHash().hasKey(key, refreshToken);
        if (exists == null || !exists) {
            throw new BizException(ErrorCode.CLIENT_ERROR, "登录已过期，请重新登录");
        }

        // 获取旧的设备信息
        String oldValue = (String) redisTemplate.opsForHash().get(key, refreshToken);
        String deviceInfo = extractDeviceInfo(oldValue);

        // 删除旧 token
        redisTemplate.opsForHash().delete(key, refreshToken);

        // 签发新 token 对
        return issueTokenPair(userId, deviceInfo);
    }

    /**
     * 登出（删除 refresh token）
     */
    public void logout(Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, refreshToken);
    }

    /**
     * 验证 refresh token 是否有效
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return false;
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String key = REFRESH_TOKEN_PREFIX + userId;
        Boolean exists = redisTemplate.opsForHash().hasKey(key, refreshToken);
        return exists != null && exists;
    }

    private void storeRefreshToken(Long userId, String refreshToken, String deviceInfo) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        try {
            String value = objectMapper.writeValueAsString(Map.of(
                    "deviceInfo", deviceInfo != null ? deviceInfo : "unknown",
                    "createdAt", LocalDateTime.now()
            ));
            redisTemplate.opsForHash().put(key, refreshToken, value);
            // 设置整个 hash 的过期时间（7天）
            redisTemplate.expire(key, jwtUtil.getRefreshExpiration(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            log.error("序列化 refresh token 信息失败", e);
        }
    }

    private String extractDeviceInfo(String value) {
        if (value == null) return "unknown";
        try {
            Map<String, Object> map = objectMapper.readValue(value, Map.class);
            return (String) map.getOrDefault("deviceInfo", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
