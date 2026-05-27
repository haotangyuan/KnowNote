package dev.haotangyuan.knownote.studio.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.haotangyuan.knownote.studio.agent.CodeGenPipeline;
import dev.haotangyuan.knownote.studio.api.dto.CreateProjectRequest;
import dev.haotangyuan.knownote.studio.client.StudioServiceClient;
import dev.haotangyuan.knownote.studio.domain.entity.StudioProjectDO;
import dev.haotangyuan.knownote.studio.domain.mapper.StudioProjectMapper;
import dev.haotangyuan.knownote.studio.sse.StudioSseHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudioProjectService {

    private final StudioProjectMapper projectMapper;
    private final CodeGenPipeline codeGenPipeline;
    private final StudioSseHub sseHub;
    private final StudioServiceClient studioServiceClient;

    public StudioProjectDO create(Long userId, CreateProjectRequest req) {
        StudioProjectDO project = new StudioProjectDO();
        project.setUserId(userId);
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        project.setStatus("ACTIVE");
        projectMapper.insert(project);
        log.info("Created studio project id={} for user={}", project.getId(), userId);
        return project;
    }

    public List<StudioProjectDO> listByUser(Long userId) {
        return projectMapper.selectList(
                new LambdaQueryWrapper<StudioProjectDO>()
                        .eq(StudioProjectDO::getUserId, userId)
                        .ne(StudioProjectDO::getStatus, "DELETED")
                        .orderByDesc(StudioProjectDO::getCreatedAt)
        );
    }

    public StudioProjectDO getById(Long projectId) {
        return projectMapper.selectById(projectId);
    }

    /**
     * Trigger code generation. Returns the SSE emitter immediately;
     * the pipeline runs in the background via @Async("studioTaskExecutor").
     */
    public SseEmitter generate(String projectId, String userMessage) {
        SseEmitter emitter = sseHub.connect(projectId);
        // Start the container asynchronously (best-effort for demo)
        new Thread(() -> studioServiceClient.startContainer(projectId)).start();
        // Run the generation pipeline in the @Async thread pool
        codeGenPipeline.run(projectId, userMessage);
        return emitter;
    }

    /**
     * Subscribe to SSE events for a project without triggering generation.
     * Used when the frontend reconnects.
     */
    public SseEmitter subscribe(String projectId) {
        return sseHub.connect(projectId);
    }

    public String getContainerStatus(String projectId) {
        return studioServiceClient.getContainerStatus(projectId);
    }
}
