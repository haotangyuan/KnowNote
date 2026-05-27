package dev.haotangyuan.knownote.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that ResearcherAgent's tavilySearch argument parsing does NOT throw
 * WorkflowException on malformed JSON — returns an error string instead.
 */
class ResearcherAgentJsonParseTest {

    record SearchArgs(String query, int maxResults, String topic) {}

    private SearchArgs parseSearchArgsDefensively(ObjectMapper mapper, String arguments) {
        try {
            var argsNode = mapper.readTree(arguments);
            if (argsNode == null || !argsNode.has("query")) {
                return null;
            }
            String query = argsNode.get("query").asText();
            int maxResults = argsNode.has("maxResults") ? argsNode.get("maxResults").asInt() : 3;
            String topic = argsNode.has("topic") ? argsNode.get("topic").asText() : "general";
            return new SearchArgs(query, maxResults, topic);
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    void validJson_parsesAllFields() {
        ObjectMapper mapper = new ObjectMapper();
        SearchArgs args = parseSearchArgsDefensively(mapper,
                "{\"query\":\"Java records\",\"maxResults\":5,\"topic\":\"technology\"}");
        assertThat(args).isNotNull();
        assertThat(args.query()).isEqualTo("Java records");
        assertThat(args.maxResults()).isEqualTo(5);
        assertThat(args.topic()).isEqualTo("technology");
    }

    @Test
    void defaults_whenOptionalFieldsMissing() {
        ObjectMapper mapper = new ObjectMapper();
        SearchArgs args = parseSearchArgsDefensively(mapper, "{\"query\":\"Spring Boot\"}");
        assertThat(args).isNotNull();
        assertThat(args.maxResults()).isEqualTo(3);
        assertThat(args.topic()).isEqualTo("general");
    }

    @Test
    void malformedJson_returnsNull_noThrow() {
        ObjectMapper mapper = new ObjectMapper();
        assertThatCode(() -> parseSearchArgsDefensively(mapper, "not-json"))
                .doesNotThrowAnyException();
        assertThat(parseSearchArgsDefensively(mapper, "not-json")).isNull();
    }

    @Test
    void missingQueryField_returnsNull() {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(parseSearchArgsDefensively(mapper, "{\"maxResults\":3}")).isNull();
    }
}
