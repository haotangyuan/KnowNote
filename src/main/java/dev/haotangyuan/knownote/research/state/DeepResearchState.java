package dev.haotangyuan.knownote.research.state;

import dev.haotangyuan.knownote.config.ResearchProperties;
import dev.haotangyuan.knownote.research.schema.ScopeSchema;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 深度研究流程状态（持久化字段，13个）
 * <p>
 * 仅包含跨阶段需要共享的持久化状态。
 * 每个代理内部的临时计数器（conductCount、searchCount、
 * researcherIterations 等）和每次搜索的临时数据
 * （searchResults、searchNotes 等）已移为各代理的局部变量。
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

    private List<String> supervisorNotes;

    private String report;

    private Long currentScopeEventId;
    private Long currentSupervisorEventId;

    private Long totalInputTokens;
    private Long totalOutputTokens;
}
