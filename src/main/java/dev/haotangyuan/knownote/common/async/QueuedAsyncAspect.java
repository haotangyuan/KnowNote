package dev.haotangyuan.knownote.common.async;

import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.common.util.SequenceUtil;
import dev.haotangyuan.knownote.common.sse.SseHub;
import dev.haotangyuan.knownote.research.data.EventType;
import dev.haotangyuan.knownote.research.data.WorkflowStatus;
import dev.haotangyuan.knownote.research.exception.ResearchException;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.domain.mapper.ResearchSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * QueuedAsync 注解切面，拦截方法调用并提交到线程池
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class QueuedAsyncAspect {

    private final ResearchTaskExecutor researchTaskExecutor;
    private final ResearchSessionMapper researchSessionMapper;
    private final EventPublisher eventPublisher;
    private final SseHub sseHub;
    private final SequenceUtil sequenceUtil;
    private final ModelHandler modelHandler;

    @Around("@annotation(QueuedAsync)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof DeepResearchState state)) {
            throw new ResearchException("@QueuedAsync 方法的第一个参数必须是 DeepResearchState");
        }

        String researchId = state.getResearchId();
        researchTaskExecutor.submit(researchId, () -> {
            try {
                joinPoint.proceed();
            } catch (Throwable e) {
                log.error("异步任务执行失败，researchId={}", researchId, e);
                handleFailure(researchId, state, e);
            }
        });
        return null;
    }

    private void handleFailure(String researchId, DeepResearchState state, Throwable e) {
        try {
            state.setStatus(WorkflowStatus.FAILED);
            researchSessionMapper.updateSession(researchId, WorkflowStatus.FAILED,
                    false, true,
                    state.getTotalInputTokens() != null ? state.getTotalInputTokens() : 0L,
                    state.getTotalOutputTokens() != null ? state.getTotalOutputTokens() : 0L);

            eventPublisher.publishEvent(researchId, EventType.ERROR,
                    "系统错误，请稍后重试", e.getMessage());

            sequenceUtil.reset(researchId);
            sseHub.complete(researchId, WorkflowStatus.FAILED);
            modelHandler.removeModel(researchId);
        } catch (Exception cleanupError) {
            log.error("异常处理过程中发生错误，researchId={}", researchId, cleanupError);
        }
    }
}
