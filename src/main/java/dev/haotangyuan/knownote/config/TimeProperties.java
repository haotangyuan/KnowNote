package dev.haotangyuan.knownote.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * 时间相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "time")
public class TimeProperties {

    private String timezone = "Asia/Shanghai";

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }
}
