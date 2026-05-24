package dev.haotangyuan.knownote.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 时间配置：JVM 默认时区 + Jackson 序列化格式
 */
@Configuration
@RequiredArgsConstructor
public class TimeConfig {

    private final TimeProperties timeProperties;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeProperties.getZoneId()));
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .serializers(
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)),
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_FORMAT)),
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT))
            )
            .deserializers(
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)),
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_FORMAT)),
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT))
            );
    }
}
