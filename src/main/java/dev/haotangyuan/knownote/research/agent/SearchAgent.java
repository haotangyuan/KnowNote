package dev.haotangyuan.knownote.research.agent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.research.client.TavilyClient;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.schema.SummarySchema;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.haotangyuan.knownote.research.prompt.SearchPrompts.SUMMARIZE_WEBPAGE_PROMPT;

/**
 * Search 阶段代理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchAgent {
    private final ModelHandler modelHandler;
    private final TavilyClient tavilyClient;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    public String run(String query, int maxResults, String topic,
                      Long parentEventId, DeepResearchState state) {
        Long searchEventId = eventPublisher.publishEvent(state.getResearchId(), EventType.SEARCH,
                "正在搜索: " + query, null, parentEventId);

        AgentAbility agent = AgentAbility.builder()
                .memory(MessageWindowChatMemory.withMaxMessages(100))
                .chatModel(modelHandler.getModel(state.getResearchId()))
                .streamingChatModel(modelHandler.getStreamModel(state.getResearchId()))
                .build();

        Map<String, TavilyClient.SearchResult> searchResults = new HashMap<>();
        List<String> searchNotes = new ArrayList<>();

        plan(query, maxResults, topic, searchEventId, state, searchResults);
        action(agent, state, searchResults, searchNotes);
        return summarize(query, searchEventId, agent, state, searchNotes);
    }

    private void plan(String query, int maxResults, String topic,
                      Long searchEventId, DeepResearchState state,
                      Map<String, TavilyClient.SearchResult> searchResults) {
        TavilyClient.TavilyResponse response = tavilyClient.search(
            query,
            maxResults,
            topic,
            true
        );

        if (response.results().isEmpty()) {
            log.warn("No search results for: {}", query);
            return;
        }

        for (TavilyClient.SearchResult result : response.results()) {
            if (result.url() != null && !searchResults.containsKey(result.url())) {
                searchResults.put(result.url(), result);
            }
        }

        eventPublisher.publishEvent(state.getResearchId(), EventType.SEARCH,
                "找到 " + searchResults.size() + " 个相关结果", null, searchEventId);
    }

    private void action(AgentAbility agent, DeepResearchState state,
                        Map<String, TavilyClient.SearchResult> searchResults,
                        List<String> searchNotes) {
        if (searchResults.isEmpty()) {
            log.warn("No search results to process");
            return;
        }

        for (TavilyClient.SearchResult result : searchResults.values()) {
            String content = result.rawContent() != null && !result.rawContent().isEmpty()
                ? result.rawContent()
                : result.content();

            if (content != null && content.length() > 500) {
                try {
                    SummarySchema summary = summarizeWebpage(agent, state, content);
                    String formatted = StrUtil.format(
                        "[{title}]\nURL: {url}\n<summary>{summary}</summary>\n<key_excerpts>{key_excerpts}</key_excerpts>",
                        Map.of(
                            "title", result.title(),
                            "url", result.url(),
                            "summary", summary.getSummary(),
                            "key_excerpts", summary.getKeyExcerpts()
                        )
                    );
                    searchNotes.add(formatted);
                } catch (Exception e) {
                    log.warn("Failed to summarize {}: {}", result.url(), e.getMessage(), e);
                    searchNotes.add(StrUtil.format("[{title}]\nURL: {url}\n{content}",
                        Map.of(
                            "title", result.title(),
                            "url", result.url(),
                            "content", result.content()
                        )));
                }
            } else {
                searchNotes.add(StrUtil.format("[{title}]\nURL: {url}\n{content}",
                    Map.of(
                        "title", result.title(),
                        "url", result.url(),
                        "content", content
                    )));
            }
        }
    }

    private SummarySchema summarizeWebpage(AgentAbility agent, DeepResearchState state, String webpageContent) {
        try {
            String prompt = StrUtil.format(SUMMARIZE_WEBPAGE_PROMPT, Map.of(
                "webpage_content", webpageContent,
                "date", DateUtil.today()
            ));

            ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .build();

            ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .responseFormat(responseFormat)
                .build();

            ChatResponse chatResponse = agent.getChatModel().chat(chatRequest);
            TokenUsage tokenUsage = chatResponse.tokenUsage();
            state.setTotalInputTokens(state.getTotalInputTokens() + tokenUsage.inputTokenCount());
            state.setTotalOutputTokens(state.getTotalOutputTokens() + tokenUsage.outputTokenCount());
            return objectMapper.readValue(chatResponse.aiMessage().text(), SummarySchema.class);

        } catch (Exception e) {
            log.error("Webpage summarization failed", e);
            SummarySchema fallback = new SummarySchema();
            fallback.setSummary(webpageContent.substring(0, Math.min(1000, webpageContent.length())));
            fallback.setKeyExcerpts("");
            return fallback;
        }
    }

    private String summarize(String query, Long searchEventId, AgentAbility agent,
                              DeepResearchState state, List<String> searchNotes) {
        if (searchNotes.isEmpty()) {
            return "No search results found for: " + query;
        }
        eventPublisher.publishEvent(state.getResearchId(), EventType.SEARCH,
                "已分析并整理搜索结果", null, searchEventId);

        StringBuilder output = new StringBuilder();
        output.append(StrUtil.format("Search results for query: '{query}'\n\n",
                Map.of("query", query)));

        int num = 1;
        for (String result : searchNotes) {
            output.append(StrUtil.format("\n--- SOURCE {index} ---\n",
                    Map.of("index", num++)));
            output.append(result);
            output.append("\n").append("-".repeat(80)).append("\n");
        }

        return output.toString();
    }
}
