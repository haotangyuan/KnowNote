package dev.haotangyuan.knownote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tavily API 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tavily")
public class TavilyProperties {
    private String apiKey;
    private String baseUrl;
}
