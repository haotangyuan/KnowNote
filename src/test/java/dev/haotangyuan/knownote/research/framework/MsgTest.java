package dev.haotangyuan.knownote.research.framework;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MsgTest {

    @Test
    void of_setsRoleNameContent() {
        Msg msg = Msg.of("user", "alice", "hello");
        assertThat(msg.role()).isEqualTo("user");
        assertThat(msg.name()).isEqualTo("alice");
        assertThat(msg.content()).isEqualTo("hello");
        assertThat(msg.id()).isNotBlank();
        assertThat(msg.metadata()).isEmpty();
    }

    @Test
    void of_withMetadata_copiesMetadataImmutably() {
        Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("key", "val");
        Msg msg = Msg.of("system", "sys", "text", meta);
        meta.put("another", "value");
        assertThat(msg.metadata()).containsOnlyKeys("key");
    }

    @Test
    void contentAsString_returnsStringRepresentation() {
        Msg msg = Msg.of("user", "u", 42);
        assertThat(msg.contentAsString()).isEqualTo("42");
    }

    @Test
    void contentAsString_nullContent_returnsEmpty() {
        Msg msg = Msg.of("user", "u", null);
        assertThat(msg.contentAsString()).isEmpty();
    }

    @Test
    void contentAs_castsToCorrectType() {
        Msg msg = Msg.of("user", "u", "hello");
        assertThat(msg.contentAs(String.class)).isEqualTo("hello");
    }

    @Test
    void contentAs_wrongType_throwsClassCastException() {
        Msg msg = Msg.of("user", "u", "not-an-integer");
        assertThatThrownBy(() -> msg.contentAs(Integer.class))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void twoMsgsWithSameData_haveDifferentIds() {
        Msg a = Msg.of("user", "u", "x");
        Msg b = Msg.of("user", "u", "x");
        assertThat(a.id()).isNotEqualTo(b.id());
    }
}
