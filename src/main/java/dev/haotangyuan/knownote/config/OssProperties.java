package dev.haotangyuan.knownote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 存储桶相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private String endpoint;
    private String publicUrl;
    private String publicBucket;
    private String privateBucket;
    private String accessKeyId;
    private String secretAccessKey;
}
