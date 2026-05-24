package dev.haotangyuan.knownote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "knownote-secret-key-change-in-production-256bit";
    /**
     * Access Token 过期时间：15分钟
     */
    private long expiration = 900000;
    /**
     * Refresh Token 过期时间：7天
     */
    private long refreshExpiration = 604800000;
}
