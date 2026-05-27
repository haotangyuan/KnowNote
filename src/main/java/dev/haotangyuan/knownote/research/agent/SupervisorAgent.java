package dev.haotangyuan.knownote.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.data.WorkflowStatus;
import dev.haotangyuan.knownote.research.exception.WorkflowException;
import dev.haotangyuan.knownote.research.framework.Agent;
import dev.haotangyuan.knownote.research.framework.AgentContext;
import dev.haotangyuan.knownote.research.framework.Msg;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.tool.ToolRegistry;
import dev.haotangyuan.knownote.research.tool.annotation.SupervisorTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
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

import static dev.haotangyuan.knownote.research.prompt.SupervisorPrompts.LEAD_RESEARCHER_PROMPT;

/**
 * Supervisor 阶段代理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupervisorAgent implements Agent {
    private final ModelHandler modelHandler;
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final ResearcherAgent researcherAgent;
    private final EventPublisher eventPublisher;

    private static final String SUPERVISOR_STAGE = SupervisorTool.class.getSimpleName();
    private static final String TOOL_REMINDER = "上一轮未实际调用任何工具。请用 thinkTool 先做规划，再以工具调用形式触发 conductResearch，想要结束时使用 researchComplete 结束。";

    public void run(DeepResearchState state) {
        state.setStatus(WorkflowStatus.IN_RESEARCH);
        Long supervisorEventId = eventPublisher.publishEvent(state.getResearchId(),
                EventType.SUPERVISOR, "开始规划研究路线...", state.getResearchBrief());
        state.setCurrentSupervisorEventId(supervisorEventId);
        AgentAbility agent = AgentAbility.builder()
                .memory(MessageWindowChatMemory.withMaxMessages(100))
                .chatModel(modelHandler.getModel(state.getResearchId()))
                .streamingChatModel(modelHandler.getStreamModel(state.getResearchId()))
                .build();
        SystemMessage systemMessage = SystemMessage.from(
                StrUtil.format(LEAD_RESEARCHER_PROMPT, Map.of(
                        "date", DateUtil.today(),
                        "max_concurrent_research_units", state.getBudget().getMaxConcurrentUnits(),
                        "max_researcher_iterations", state.getBudget().getMaxConductCount()
                )));
        agent.getMemory().add(systemMessage);
        agent.getMemory().add(UserMessage.from(state.getResearchBrief()));
        plan(agent, state);
    }

    private void plan(AgentAbility agent, DeepResearchState state) {
        int maxConductCount = state.getBudget().getMaxConductCount();
        int maxIterations = maxConductCount * 2;
        int conductCount = 0;
        int supervisorIterations = 0;
        while (conductCount < maxConductCount && supervisorIterations < maxIterations) {
            List<ToolSpecification> toolSpecifications = toolRegistry.getToolSpecifications(SUPERVISOR_STAGE);
            ChatRequest chatRequest = ChatRequest.builder()
                    .toolSpecifications(toolSpecifications)
                    .toolChoice(ToolChoice.REQUIRED)
                    .messages(agent.getMemory().messages())
                    .build();
            ChatResponse chatResponse = agent.getChatModel().chat(chatRequest);
            TokenUsage tokenUsage = chatResponse.tokenUsage();
            state.setTotalInputTokens(state.getTotalInputTokens() + tokenUsage.inputTokenCount());
            state.setTotalOutputTokens(state.getTotalOutputTokens() + tokenUsage.outputTokenCount());
            agent.getMemory().add(chatResponse.aiMessage());

            List<ToolExecutionRequest> toolExecutionRequests = chatResponse.aiMessage().toolExecutionRequests();
            if (toolExecutionRequests == null || toolExecutionRequests.isEmpty()) {
                agent.getMemory().add(UserMessage.from(TOOL_REMINDER));
                supervisorIterations++;
                continue;
            }

            conductCount = action(agent, toolExecutionRequests, state, conductCount, maxConductCount);

            // P5 fix: hard-exit when conduct quota is exhausted
            if (conductCount >= maxConductCount) {
                log.info("Conduct quota exhausted ({}/{}), exiting supervisor loop for researchId={}",
                        conductCount, maxConductCount, state.getResearchId());
                break;
            }

            if (toolExecutionRequests.stream()
                    .anyMatch(toolRequest -> "researchComplete".equals(toolRequest.name()))) {
                break;
            }

            supervisorIterations++;
        }
    }

    /** Returns the updated conductCount. */
    private int action(AgentAbility agent, List<ToolExecutionRequest> toolExecutionRequests,
                       DeepResearchState state, int conductCount, int maxConductCount) {
        if (toolExecutionRequests == null || toolExecutionRequests.isEmpty()) {
            return conductCount;
        }
        for (ToolExecutionRequest toolExecutionRequest : toolExecutionRequests) {
            String result;

            if ("conductResearch".equals(toolExecutionRequest.name())) {
                if (conductCount >= maxConductCount) {
                    log.warn("conductResearch count limit reached: {}/{}",
                            conductCount, maxConductCount);
                    result = "已达到研究任务配额限制，请调用 researchComplete 完成研究";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
                    continue;
                }

                String researchTopic;
                try {
                    var argsNode = objectMapper.readTree(toolExecutionRequest.arguments());
                    researchTopic = (argsNode != null && argsNode.has("researchTopic")) ? argsNode.get("researchTopic").asText() : null;
                } catch (Exception e) {
                    log.error("Failed to parse conductResearch arguments for researchId={}", state.getResearchId(), e);
                    String errResult = "工具参数解析失败，请重新调用并提供正确的JSON格式";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, errResult));
                    continue;
                }
                if (researchTopic == null || researchTopic.isBlank()) {
                    String errResult = "缺少必填参数 researchTopic，请重新调用并提供研究主题";
                    agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, errResult));
                    continue;
                }

                Long planEventId = eventPublisher.publishEvent(state.getResearchId(), EventType.SUPERVISOR,
                        "正在研究: " + researchTopic, null, state.getCurrentSupervisorEventId());

                result = researcherAgent.run(state, researchTopic, planEventId);

                conductCount++;
            } else {
                var executor = toolRegistry.getExecutor(toolExecutionRequest.name());
                if (executor == null) {
                    log.warn("No executor found for tool {} in stage {}", toolExecutionRequest.name(), SUPERVISOR_STAGE);
                    continue;
                }
                result = executor.execute(toolExecutionRequest, null);
            }

            if (toolExecutionRequest.name().equals("thinkTool")) {
                eventPublisher.publishEvent(state.getResearchId(), EventType.SUPERVISOR,
                        "思考中...", result, state.getCurrentSupervisorEventId());
                state.getSupervisorNotes().add(result);
            } else if (toolExecutionRequest.name().equals("conductResearch")) {
                state.getSupervisorNotes().add(result);
            }

            agent.getMemory().add(ToolExecutionResultMessage.from(toolExecutionRequest, result));
        }
        return conductCount;
    }

    // ── Agent interface ─────────────────────────────────────────────────────

    @Override
    public String name() {
        return "supervisor-agent";
    }

    @Override
    public Msg reply(Msg input, AgentContext ctx) {
        DeepResearchState state = input.contentAs(DeepResearchState.class);
        run(state);
        return Msg.of("assistant", name(), state.getSupervisorNotes());
    }
}
