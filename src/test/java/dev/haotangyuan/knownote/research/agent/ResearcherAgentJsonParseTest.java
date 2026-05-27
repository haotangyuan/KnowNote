package dev.haotangyuan.knownote.research.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that ResearcherAgent.parseSearchArgs() — the actual production
 * parsing helper used inside action() — handles all input variants correctly.
 */
class ResearcherAgentJsonParseTest {

    @Test
    void validJson_parsesAllFields() {
        ObjectMapper mapper = new ObjectMapper();
        ResearcherAgent.SearchArgs args = ResearcherAgent.parseSearchArgs(mapper,
                "{\"query\":\"Java records\",\"maxResults\":5,\"topic\":\"technology\"}");
        assertThat(args).isNotNull();
        assertThat(args.query()).isEqualTo("Java records");
        assertThat(args.maxResults()).isEqualTo(5);
        assertThat(args.topic()).isEqualTo("technology");
    }

    @Test
    void defaults_whenOptionalFieldsMissing() {
        ObjectMapper mapper = new ObjectMapper();
        ResearcherAgent.SearchArgs args = ResearcherAgent.parseSearchArgs(mapper,
                "{\"query\":\"Spring Boot\"}");
        assertThat(args).isNotNull();
        assertThat(args.maxResults()).isEqualTo(3);
        assertThat(args.topic()).isEqualTo("general");
    }

    @Test
    void malformedJson_returnsNull_noThrow() {
        ObjectMapper mapper = new ObjectMapper();
        assertThatCode(() -> ResearcherAgent.parseSearchArgs(mapper, "not-json"))
                .doesNotThrowAnyException();
        assertThat(ResearcherAgent.parseSearchArgs(mapper, "not-json")).isNull();
    }

    @Test
    void missingQueryField_returnsNull() {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(ResearcherAgent.parseSearchArgs(mapper, "{\"maxResults\":3}")).isNull();
    }

    @Test
    void blankQuery_returnsNull() {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(ResearcherAgent.parseSearchArgs(mapper, "{\"query\":\"\"}")).isNull();
    }
}
