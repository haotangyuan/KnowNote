package dev.haotangyuan.knownote.research.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.config.TavilyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.io.IOException;
import java.util.List;

/**
 * Tavily 搜索 API 客户端
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TavilyClient {
    private final TavilyProperties tavilyProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TavilyResponse search(String query, int maxResults, String topic, boolean includeRawContent) {
        try {
            TavilyRequest request = new TavilyRequest(
                query, maxResults, topic, includeRawContent
            );

            String json = objectMapper.writeValueAsString(request);

            log.debug("Tavily search: query='{}', maxResults={}, topic='{}'", query, maxResults, topic);

            try (HttpResponse response = HttpRequest.post(tavilyProperties.getBaseUrl() + "/search")
                .header("Authorization", "Bearer " + tavilyProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(json)
                .timeout(10_000)
                .execute()) {
                if (!response.isOk() || response.body() == null) {
                    log.error("Tavily API failed: code={}", response.getStatus());
                    return new TavilyResponse(List.of());
                }
                return objectMapper.readValue(response.body(), TavilyResponse.class);
            }
        } catch (IOException e) {
            log.error("Tavily search failed for: {}", query, e);
            return new TavilyResponse(List.of());
        }
    }

    public record TavilyRequest(
        String query,
        @JsonProperty("max_results") int maxResults,
        String topic,
        @JsonProperty("include_raw_content") boolean includeRawContent
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TavilyResponse(List<SearchResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResult(
        String url,
        String title,
        String content,
        @JsonProperty("raw_content") String rawContent,
        Double score
    ) {}
}
