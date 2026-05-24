package dev.haotangyuan.knownote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Research 模块配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "research")
public class ResearchProperties {

    private Async async = new Async();
    private Budget budget = new Budget();
    private Model model = new Model();

    @Data
    public static class Async {
        private int maxPoolSize = 10;
        private int queueCapacity = 50;
        private int taskTimeoutMinutes = 3;
    }

    @Data
    public static class Budget {
        private Map<String, BudgetLevel> levels;

        public BudgetLevel getLevel(String level) {
            if (levels == null || level == null) {
                return null;
            }
            return levels.get(level.toUpperCase());
        }
    }

    @Data
    public static class BudgetLevel {
        private int maxConductCount;
        private int maxSearchCount;
        private int maxConcurrentUnits;
    }

    @Data
    public static class Model {
        private String id;
        private String name;
        private String model;
        private String baseUrl;
        private String apiKey;
    }
}
