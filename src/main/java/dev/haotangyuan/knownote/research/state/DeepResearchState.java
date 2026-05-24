package dev.haotangyuan.knownote.research.state;

import dev.haotangyuan.knownote.config.ResearchProperties;
import dev.haotangyuan.knownote.research.client.TavilyClient;
import dev.haotangyuan.knownote.research.schema.ScopeSchema;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 深度研究流程状态
 */
@Data
@Builder
public class DeepResearchState {

    private String researchId;
    private List<ChatMessage> chatHistory;
    private String status;

    private ScopeSchema.ClarifyWithUserSchema clarifyWithUserSchema;
    private ScopeSchema.ResearchQuestion researchQuestion;
    private String researchBrief;

    private ResearchProperties.BudgetLevel budget;

    private Integer supervisorIterations;
    private Integer conductCount;
    private List<String> supervisorNotes;

    private String researchTopic;
    private Integer researcherIterations;
    private Integer searchCount;
    private List<String> researcherNotes;
    private String compressedResearch;

    private String query;
    private Integer maxResults;
    private String topic;
    private Map<String, TavilyClient.SearchResult> searchResults;
    private List<String> searchNotes;

    private String report;

    private Long currentScopeEventId;
    private Long currentSupervisorEventId;
    private Long currentResearchEventId;
    private Long currentSearchEventId;

    private Long totalInputTokens;
    private Long totalOutputTokens;
}
