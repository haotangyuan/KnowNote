package dev.haotangyuan.knownote.research.workflow;

import dev.haotangyuan.knownote.common.async.QueuedAsync;
import dev.haotangyuan.knownote.common.sse.SseHub;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.common.util.SequenceUtil;
import dev.haotangyuan.knownote.research.agent.ScopeAgent;
import dev.haotangyuan.knownote.research.agent.SupervisorAgent;
import dev.haotangyuan.knownote.research.agent.ReportAgent;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.data.WorkflowStatus;
import dev.haotangyuan.knownote.research.exception.WorkflowException;
import dev.haotangyuan.knownote.research.framework.AgentContext;
import dev.haotangyuan.knownote.research.framework.Msg;
import dev.haotangyuan.knownote.research.framework.SequentialPipeline;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.domain.mapper.ResearchSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 研究流程编排器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentPipeline {
    private final ScopeAgent scopeAgent;
    private final SupervisorAgent supervisorAgent;
    private final ReportAgent reportAgent;
    private final SequenceUtil sequenceUtil;
    private final SseHub sseHub;
    private final ResearchSessionMapper researchSessionMapper;
    private final EventPublisher eventPublisher;
    private final ModelHandler modelHandler;

    @QueuedAsync
    public void run(DeepResearchState state) {
        String researchId = state.getResearchId();
        AgentContext ctx = AgentContext.builder()
                .researchId(researchId)
                .budget(state.getBudget())
                .build();
        try {
            state.setStatus(WorkflowStatus.START);
            updateResearchSession(researchId, WorkflowStatus.START, state);

            SequentialPipeline pipeline = new SequentialPipeline(
                    List.of(scopeAgent, supervisorAgent, reportAgent));
            pipeline.run(Msg.of("user", "system", state), ctx);

            String finalStatus = state.getStatus();
            if (WorkflowStatus.NEED_CLARIFICATION.equals(finalStatus)) {
                log.info("Scope phase requires clarification for researchId={}", researchId);
                updateResearchSession(researchId, WorkflowStatus.NEED_CLARIFICATION, state);
            } else if (WorkflowStatus.FAILED.equals(finalStatus)) {
                log.warn("Pipeline failed for researchId={}", researchId);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "研究过程中发生错误", null);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
            } else if (WorkflowStatus.IN_REPORT.equals(finalStatus)) {
                state.setStatus(WorkflowStatus.COMPLETED);
                updateResearchSession(researchId, WorkflowStatus.COMPLETED, state);
                log.info("Final report generated for researchId={}", researchId);
            } else {
                log.warn("Unexpected final status for researchId={}, status={}", researchId, finalStatus);
                state.setStatus(WorkflowStatus.FAILED);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "研究过程中状态异常", "status=" + finalStatus);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
            }
        } catch (WorkflowException e) {
            state.setStatus(WorkflowStatus.FAILED);
            eventPublisher.publishEvent(researchId, EventType.ERROR,
                    "研究过程中发生错误", null);
            updateResearchSession(researchId, WorkflowStatus.FAILED, state);
            log.error("Workflow failed for researchId={}, error={}", researchId, e.getMessage(), e);
        } catch (Exception e) {
            state.setStatus(WorkflowStatus.FAILED);
            eventPublisher.publishEvent(researchId, EventType.ERROR,
                    "系统错误，请稍后重试", null);
            updateResearchSession(researchId, WorkflowStatus.FAILED, state);
            log.error("Unexpected error for researchId={}", researchId, e);
        } finally {
            sequenceUtil.reset(researchId);
            sseHub.complete(researchId, state.getStatus());
            modelHandler.removeModel(researchId);
        }
    }

    private void updateResearchSession(String researchId, String status, DeepResearchState state) {
        boolean setStartTime = WorkflowStatus.START.equals(status);
        boolean setCompleteTime = WorkflowStatus.COMPLETED.equals(status)
                || WorkflowStatus.FAILED.equals(status)
                || WorkflowStatus.NEED_CLARIFICATION.equals(status);
        researchSessionMapper.updateSession(researchId, status, setStartTime, setCompleteTime,
                state.getTotalInputTokens(), state.getTotalOutputTokens());
    }
}
