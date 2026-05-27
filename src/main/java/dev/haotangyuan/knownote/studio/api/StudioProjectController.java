package dev.haotangyuan.knownote.studio.api;

import java.util.List;
import java.util.Map;

import dev.haotangyuan.knownote.studio.api.dto.CreateProjectRequest;
import dev.haotangyuan.knownote.studio.api.dto.GenerateRequest;
import dev.haotangyuan.knownote.studio.domain.entity.StudioProjectDO;
import dev.haotangyuan.knownote.studio.service.StudioProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Studio REST API — no authentication for demo.
 */
@RestController
@RequestMapping("/api/v1/studio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudioProjectController {

    private final StudioProjectService projectService;

    // ── Project CRUD ─────────────────────────────────────────────────────────

    @PostMapping("/projects")
    public StudioProjectDO createProject(@RequestBody CreateProjectRequest req) {
        // For demo: hardcoded userId=1. Replace with UserContext.getUserId() if auth is enabled.
        return projectService.create(1L, req);
    }

    @GetMapping("/projects")
    public List<StudioProjectDO> listProjects() {
        return projectService.listByUser(1L);
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<StudioProjectDO> getProject(@PathVariable Long projectId) {
        StudioProjectDO project = projectService.getById(projectId);
        return project != null
                ? ResponseEntity.ok(project)
                : ResponseEntity.notFound().build();
    }

    // ── Code generation ───────────────────────────────────────────────────────

    /**
     * POST /api/v1/studio/projects/{projectId}/generate
     * Returns an SSE stream; generation starts immediately in the background.
     */
    @PostMapping(value = "/projects/{projectId}/generate",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(
            @PathVariable String projectId,
            @RequestBody GenerateRequest req
    ) {
        return projectService.generate(projectId, req.getMessage());
    }

    /**
     * GET /api/v1/studio/projects/{projectId}/events
     * SSE subscription without triggering generation (for reconnect).
     */
    @GetMapping(value = "/projects/{projectId}/events",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String projectId) {
        return projectService.subscribe(projectId);
    }

    // ── Container status ──────────────────────────────────────────────────────

    @GetMapping(value = "/projects/{projectId}/container/status",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public String containerStatus(@PathVariable String projectId) {
        return projectService.getContainerStatus(projectId);
    }

    // ── Dev health ────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "module", "studio");
    }
}
