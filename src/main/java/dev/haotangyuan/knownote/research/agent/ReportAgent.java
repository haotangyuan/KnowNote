package dev.haotangyuan.knownote.research.agent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.data.WorkflowStatus;
import dev.haotangyuan.knownote.research.framework.Agent;
import dev.haotangyuan.knownote.research.framework.AgentContext;
import dev.haotangyuan.knownote.research.framework.Msg;
import dev.haotangyuan.knownote.research.framework.ServiceResponse;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static dev.haotangyuan.knownote.research.prompt.ReportPrompts.REPORT_AGENT_PROMPT;

/**
 * Report 阶段代理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportAgent implements Agent {
    private final ModelHandler modelHandler;
    private final EventPublisher eventPublisher;

    public String run(DeepResearchState state) {
        state.setStatus(WorkflowStatus.IN_REPORT);
        eventPublisher.publishEvent(state.getResearchId(),
                EventType.REPORT, "正在生成研究报告...", null);
        AgentAbility agent = AgentAbility.builder()
                .memory(MessageWindowChatMemory.withMaxMessages(100))
                .chatModel(modelHandler.getModel(state.getResearchId()))
                .streamingChatModel(modelHandler.getStreamModel(state.getResearchId()))
                .build();
        UserMessage userMessage = UserMessage.from(
            StrUtil.format(REPORT_AGENT_PROMPT, Map.of(
                "research_brief", state.getResearchBrief(),
                "date", DateUtil.today(),
                "findings", StrUtil.join("\n", state.getSupervisorNotes())
            )));
        agent.getMemory().add(userMessage);
        action(agent, state);
        return state.getReport();
    }

    public void action(AgentAbility agent, DeepResearchState state) {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(agent.getMemory().messages())
                .build();
        ChatResponse chatResponse = agent.getChatModel().chat(chatRequest);
        TokenUsage tokenUsage = chatResponse.tokenUsage();
        state.setTotalInputTokens(state.getTotalInputTokens() + tokenUsage.inputTokenCount());
        state.setTotalOutputTokens(state.getTotalOutputTokens() + tokenUsage.outputTokenCount());
        agent.getMemory().add(chatResponse.aiMessage());
        state.setReport(chatResponse.aiMessage().text());
        eventPublisher.publishEvent(state.getResearchId(), EventType.REPORT,
                "研究报告已完成", null);
        eventPublisher.publishMessage(state.getResearchId(), "assistant", chatResponse.aiMessage().text());
    }

    // ── Agent interface ─────────────────────────────────────────────────────

    @Override
    public String name() {
        return "report-agent";
    }

    @Override
    public Msg reply(Msg input, AgentContext ctx) {
        DeepResearchState state = input.contentAs(DeepResearchState.class);
        run(state);
        String status = state.getStatus();
        if (!WorkflowStatus.IN_REPORT.equals(status)) {
            return Msg.of("assistant", name(), ServiceResponse.error(status));
        }
        return Msg.of("assistant", name(), state);
    }
}
