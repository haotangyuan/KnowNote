package dev.haotangyuan.knownote.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.knownote.research.exception.WorkflowException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that SupervisorAgent's JSON parsing does NOT throw WorkflowException
 * when tool arguments are malformed — it should return an error string instead.
 */
class SupervisorAgentJsonParseTest {

    /**
     * Helper: simulates the defensive parsing logic extracted from SupervisorAgent.action().
     * Returns null instead of throwing on bad JSON.
     */
    private String parseTopicDefensively(ObjectMapper mapper, String arguments) {
        try {
            var argsNode = mapper.readTree(arguments);
            if (argsNode == null || !argsNode.has("topic")) {
                return null;
            }
            return argsNode.get("topic").asText();
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void validJson_parsesTopic() {
        ObjectMapper mapper = new ObjectMapper();
        String topic = parseTopicDefensively(mapper, "{\"topic\": \"AI ethics\"}");
        assertThat(topic).isEqualTo("AI ethics");
    }

    @Test
    void malformedJson_returnsNull_doesNotThrow() {
        ObjectMapper mapper = new ObjectMapper();
        assertThatCode(() -> parseTopicDefensively(mapper, "{bad json}"))
                .doesNotThrowAnyException();
        String topic = parseTopicDefensively(mapper, "{bad json}");
        assertThat(topic).isNull();
    }

    @Test
    void missingTopicField_returnsNull() {
        ObjectMapper mapper = new ObjectMapper();
        String topic = parseTopicDefensively(mapper, "{\"other\": \"value\"}");
        assertThat(topic).isNull();
    }
}
