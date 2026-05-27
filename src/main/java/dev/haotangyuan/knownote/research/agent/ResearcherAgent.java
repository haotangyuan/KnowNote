package dev.haotangyuan.knownote.research.agent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.framework.Agent;
import dev.haotangyuan.knownote.research.framework.AgentContext;
import dev.haotangyuan.knownote.research.framework.Msg;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.tool.ToolRegistry;
import dev.haotangyuan.knownote.research.tool.annotation.ResearcherTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.haotangyuan.knownote.research.prompt.ResearcherPrompts.*;

/**
 * Researcher 阶段代理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResearcherAgent implements Agent {
    private final ModelHandler modelHandler;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final SearchAgent searchAgent;
    private final EventPublisher eventPublisher;

    private static final String RESEARCHER_STAGE = ResearcherTool.class.getSimpleName();
    private static final String TOOL_TAVILY_SEARCH = "tavilySearch";
    private static final String TOOL_THINK = "thinkTool";

    /** Package-private for testing. Returns null on parse failure or missing query. */
    static SearchArgs parseSearchArgs(ObjectMapper objectMapper, String arguments) {
        try {
            var argsNode = objectMapper.readTree(arguments);
            if (argsNode == null || !argsNode.has("query")) {
                return null;
            }
            String query = argsNode.get("query").asText();
            if (cn.hutool.core.util.StrUtil.isBlank(query)) {
                return null;
            }
            int maxResults = argsNode.has("maxResults") ? argsNode.get("maxResults").asInt() : 3;
            String topic = argsNode.has("topic") ? argsNode.get("topic").asText() : "general";
            return new SearchArgs(query, maxResults, topic);
        } catch (Exception e) {
            return null;
        }
    }

    record SearchArgs(String query, int maxResults, String topic) {}

    public String run(DeepResearchState state, String researchTopic, Long parentEventId) {
        log.info("ResearcherAgent run: researchId='{}', topic='{}'", state.getResearchId(), researchTopic);
        Long researchEventId = eventPublisher.publishEvent(state.getResearchId(), EventType.RESEARCH,
                "深入研究: " + researchTopic, null, parentEventId);

        AgentAbility agent = AgentAbility.builder()
                .memory(MessageWindowChatMemory.withMaxMessages(100))
                .chatModel(modelHandler.getModel(state.getResearchId()))
                .streamingChatModel(modelHandler.getStreamModel(state.getResearchId()))
                .build();

        SystemMessage systemMessage = SystemMessage.from(
            StrUtil.format(RESEARCH_AGENT_PROMPT, Map.of("date", DateUtil.today()))
        );
        agent.getMemory().add(systemMessage);
        agent.getMemory().add(UserMessage.from(researchTopic));

        plan(agent, state, researchTopic, researchEventId);
        return compressResearch(agent, state, researchTopic, researchEventId);
    }

    private void plan(AgentAbility agent, DeepResearchState state,
                      String researchTopic, Long researchEventId) {
        int maxSearchCount = state.getBudget().getMaxSearchCount();
        int maxIterations = maxSearchCount * 2;
        int searchCount = 0;
        int researcherIterations = 0;
        while (searchCount < maxSearchCount && researcherIterations < maxIterations) {
            List<ToolSpecification> toolSpecifications = toolRegistry.getToolSpecifications(RESEARCHER_STAGE);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(agent.getMemory().messages())
                    .toolSpecifications(toolSpecifications)
                    .toolChoice(ToolChoice.REQUIRED)
                    .build();
            ChatResponse chatResponse = agent.getChatModel().chat(chatRequest);
            TokenUsage tokenUsage = chatResponse.tokenUsage();
            state.setTotalInputTokens(state.getTotalInputTokens() + tokenUsage.inputTokenCount());
            state.setTotalOutputTokens(state.getTotalOutputTokens() + tokenUsage.outputTokenCount());
            agent.getMemory().add(chatResponse.aiMessage());

            searchCount = action(agent, chatResponse.aiMessage().toolExecutionRequests(), state,
                    searchCount, maxSearchCount, researchEventId);

            if (!chatResponse.aiMessage().hasToolExecutionRequests()) {
                break;
            }

            researcherIterations++;
        }
    }

    /** Returns the updated searchCount. */
    private int action(AgentAbility agent, List<ToolExecutionRequest> toolExecutionRequests,
                       DeepResearchState state, int searchCount, int maxSearchCount,
                       Long researchEventId) {
        if (toolExecutionRequests == null || toolExecutionRequests.isEmpty()) {
            return searchCount;
        }

        for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequests) {
            String result = "";

            if (TOOL_TAVILY_SEARCH.equals(toolExecutionRequest.name())) {
                if (searchCount >= maxSearchCount) {
                    log.warn("tavilySearch count limit reached: {}/{}",
                            searchCount, maxSearchCount);
                    result = "已达到搜索配额限制，请根据已有信息完成研究";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
                    continue;
                }

                SearchArgs args = parseSearchArgs(objectMapper, toolExecutionRequest.arguments());
                if (args == null) {
                    log.error("Failed to parse tavilySearch arguments for researchId={}", state.getResearchId());
                    result = "工具参数解析失败或缺少必填参数 query，请重新调用并提供正确的JSON格式";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
                    continue;
                }

                result = searchAgent.run(args.query(), args.maxResults(), args.topic(),
                        researchEventId, state);
                searchCount++;
            } else {
                var executor = toolRegistry.getExecutor(toolExecutionRequest.name());
                if (executor == null) {
                    log.warn("No executor found for tool {} in stage {}", toolExecutionRequest.name(), RESEARCHER_STAGE);
                    result = "Tool not available";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
                    continue;
                }
                result = executor.execute(toolExecutionRequest, null);
            }

            if (TOOL_THINK.equals(toolExecutionRequest.name())) {
                eventPublisher.publishEvent(state.getResearchId(), EventType.RESEARCH,
                        "分析中...", result, researchEventId);
            }

            agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
        }
        return searchCount;
    }

    private String compressResearch(AgentAbility agent, DeepResearchState state,
                                    String researchTopic, Long researchEventId) {
        String systemPrompt = StrUtil.format(COMPRESS_RESEARCH_SYSTEM_PROMPT, Map.of("date", DateUtil.today()));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.addAll(agent.getMemory().messages().stream().skip(2).collect(Collectors.toList()));
        messages.add(UserMessage.from(
            StrUtil.format(COMPRESS_RESEARCH_HUMAN_MESSAGE, Map.of("research_topic", researchTopic))));

        ChatRequest compressRequest = ChatRequest.builder()
                .messages(messages)
                .build();

        ChatResponse compressResponse = agent.getChatModel().chat(compressRequest);
        TokenUsage tokenUsage = compressResponse.tokenUsage();
        state.setTotalInputTokens(state.getTotalInputTokens() + tokenUsage.inputTokenCount());
        state.setTotalOutputTokens(state.getTotalOutputTokens() + tokenUsage.outputTokenCount());
        String compressedResearch = compressResponse.aiMessage().text();

        String preview = compressedResearch.length() > 200
                ? compressedResearch.substring(0, 200) + "..."
                : compressedResearch;
        eventPublisher.publishEvent(state.getResearchId(), EventType.RESEARCH,
                "已完成该主题研究", preview, researchEventId);

        return compressedResearch;
    }

    // ── Agent interface ─────────────────────────────────────────────────────

    @Override
    public String name() {
        return "researcher-agent";
    }

    @Override
    public Msg reply(Msg input, AgentContext ctx) {
        DeepResearchState state = input.contentAs(DeepResearchState.class);
        String researchTopic = (String) input.metadata().get("researchTopic");
        Long parentEventId = (Long) input.metadata().get("parentEventId");
        String result = run(state, researchTopic, parentEventId);
        return Msg.of("assistant", name(), result);
    }
}
