package dev.haotangyuan.knownote.research.state;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test: verifies that DeepResearchState contains exactly the 13
 * persistent fields and none of the 14 transient per-call fields that were
 * removed during the P3 refactoring.
 */
class DeepResearchStateSlimTest {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "researchId",
            "chatHistory",
            "status",
            "clarifyWithUserSchema",
            "researchQuestion",
            "researchBrief",
            "budget",
            "supervisorNotes",
            "report",
            "currentScopeEventId",
            "currentSupervisorEventId",
            "totalInputTokens",
            "totalOutputTokens"
    );

    private static final Set<String> REMOVED_FIELDS = Set.of(
            "supervisorIterations",
            "conductCount",
            "researchTopic",
            "researcherIterations",
            "searchCount",
            "researcherNotes",
            "compressedResearch",
            "query",
            "maxResults",
            "topic",
            "searchResults",
            "searchNotes",
            "currentResearchEventId",
            "currentSearchEventId"
    );

    @Test
    void requiredFields_allPresent() {
        Set<String> actualFieldNames = getDeclaredFieldNames();
        assertThat(actualFieldNames)
                .as("DeepResearchState must contain all 13 required persistent fields")
                .containsAll(REQUIRED_FIELDS);
    }

    @Test
    void removedFields_nonePresent() {
        Set<String> actualFieldNames = getDeclaredFieldNames();
        assertThat(actualFieldNames)
                .as("DeepResearchState must NOT contain any of the 14 removed transient fields")
                .doesNotContainAnyElementsOf(REMOVED_FIELDS);
    }

    private Set<String> getDeclaredFieldNames() {
        return Arrays.stream(DeepResearchState.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
