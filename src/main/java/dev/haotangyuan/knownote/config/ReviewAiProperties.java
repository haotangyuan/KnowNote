package dev.haotangyuan.knownote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 发布审核 AI
 */
@Data
@Component
@ConfigurationProperties(prefix = "review-ai")
public class ReviewAiProperties {
    private Boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
}
