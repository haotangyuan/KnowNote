package dev.haotangyuan.knownote.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.common.util.EventPublisher;
import dev.haotangyuan.knownote.research.framework.Agent;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that all four pipeline agents implement the Agent interface
 * and each returns a distinct, non-blank name().
 */
@ExtendWith(MockitoExtension.class)
class AgentInterfaceTest {

    @Mock ModelHandler modelHandler;
    @Mock ObjectMapper objectMapper;
    @Mock EventPublisher eventPublisher;
    @Mock ToolRegistry toolRegistry;
    @Mock SearchAgent searchAgentMock;

    // ── interface contract ──────────────────────────────────────────────────

    @Test
    void scopeAgent_implementsAgent() {
        assertThat(Agent.class.isAssignableFrom(ScopeAgent.class)).isTrue();
    }

    @Test
    void supervisorAgent_implementsAgent() {
        assertThat(Agent.class.isAssignableFrom(SupervisorAgent.class)).isTrue();
    }

    @Test
    void researcherAgent_implementsAgent() {
        assertThat(Agent.class.isAssignableFrom(ResearcherAgent.class)).isTrue();
    }

    @Test
    void reportAgent_implementsAgent() {
        assertThat(Agent.class.isAssignableFrom(ReportAgent.class)).isTrue();
    }

    // ── name() contract ─────────────────────────────────────────────────────

    @Test
    void allAgentNames_distinctAndNonBlank() {
        Agent scope      = new ScopeAgent(modelHandler, objectMapper, eventPublisher);
        Agent supervisor = new SupervisorAgent(modelHandler, objectMapper, toolRegistry,
                new ResearcherAgent(modelHandler, toolRegistry, objectMapper, searchAgentMock, eventPublisher),
                eventPublisher);
        Agent researcher = new ResearcherAgent(modelHandler, toolRegistry, objectMapper,
                searchAgentMock, eventPublisher);
        Agent report     = new ReportAgent(modelHandler, eventPublisher);

        Set<String> names = Set.of(scope.name(), supervisor.name(), researcher.name(), report.name());

        assertThat(scope.name()).isNotBlank();
        assertThat(supervisor.name()).isNotBlank();
        assertThat(researcher.name()).isNotBlank();
        assertThat(report.name()).isNotBlank();

        assertThat(names)
                .as("Each agent must have a unique name")
                .hasSize(4);
    }
}
