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
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.domain.mapper.ResearchSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        try {
            state.setStatus(WorkflowStatus.START);
            updateResearchSession(researchId, WorkflowStatus.START, state);

            scopeAgent.run(state);

            String status = state.getStatus();
            if (WorkflowStatus.FAILED.equals(status)) {
                log.warn("Scope phase failed for researchId={}, status={} ", researchId, status);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "范围分析失败", null);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }
            if (WorkflowStatus.NEED_CLARIFICATION.equals(status)) {
                log.info("Scope phase requires clarification for researchId={}", researchId);
                updateResearchSession(researchId, WorkflowStatus.NEED_CLARIFICATION, state);
                return;
            }
            if (!WorkflowStatus.IN_SCOPE.equals(status)) {
                log.warn("Unexpected status after Scope phase for researchId={}, status={}", researchId, status);
                state.setStatus(WorkflowStatus.FAILED);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "范围分析状态异常", "status=" + status);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }

            supervisorAgent.run(state);

            status = state.getStatus();
            if (WorkflowStatus.FAILED.equals(status)) {
                log.warn("Supervisor phase failed for researchId={}, status={}", researchId, status);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "研究规划失败", null);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }
            if (!WorkflowStatus.IN_RESEARCH.equals(status)) {
                log.warn("Unexpected status after Supervisor phase for researchId={}, status={}", researchId, status);
                state.setStatus(WorkflowStatus.FAILED);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "研究规划状态异常", "status=" + status);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }

            reportAgent.run(state);

            status = state.getStatus();
            if (WorkflowStatus.FAILED.equals(status)) {
                log.warn("Report phase failed for researchId={}, status={}", researchId, status);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "报告生成失败", null);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }
            if (!WorkflowStatus.IN_REPORT.equals(status)) {
                log.warn("Unexpected status after Report phase for researchId={}, status={}", researchId, status);
                state.setStatus(WorkflowStatus.FAILED);
                eventPublisher.publishEvent(researchId, EventType.ERROR, "报告生成状态异常", "status=" + status);
                updateResearchSession(researchId, WorkflowStatus.FAILED, state);
                return;
            }

            state.setStatus(WorkflowStatus.COMPLETED);
            updateResearchSession(researchId, WorkflowStatus.COMPLETED, state);
            log.info("Final report generated for researchId={}", researchId);
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
