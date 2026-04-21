package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalHandler;
import io.ozie.jdamodals.ModalModel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlModalHandlerTest {

    private final YamlModalLoader loader = new YamlModalLoader();

    @Test
    void values_of_extracts_annotated_fields_in_declaration_order() throws Exception {
        String yaml = """
                id: form
                title: Form
                fields:
                  - name: firstName
                    type: text
                    label: First
                  - name: lastName
                    type: text
                    label: Last
                  - name: age
                    type: text
                    label: Age
                    javaType: int
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        ModalModel instance = clazz.getDeclaredConstructor().newInstance();
        set(instance, "firstName", "Ada");
        set(instance, "lastName", "Lovelace");
        set(instance, "age", 36);

        Map<String, Object> values = YamlModalHandler.valuesOf(instance);

        assertEquals(3, values.size());
        assertEquals("Ada", values.get("firstName"));
        assertEquals("Lovelace", values.get("lastName"));
        assertEquals(36, values.get("age"));
        assertTrue(values instanceof java.util.LinkedHashMap<?, ?>, "expected insertion-order map");
    }

    @Test
    void values_of_keeps_null_for_unfilled_optional_fields() throws Exception {
        String yaml = """
                id: partial
                title: Partial
                fields:
                  - name: nickname
                    type: text
                    label: Nickname
                    required: false
                """;
        Class<? extends ModalModel> clazz = loader.load(yaml);
        ModalModel instance = clazz.getDeclaredConstructor().newInstance();

        Map<String, Object> values = YamlModalHandler.valuesOf(instance);

        assertTrue(values.containsKey("nickname"));
        assertNull(values.get("nickname"));
    }

    @Test
    void handler_delegates_to_consumer_with_map() throws Exception {
        String yaml = """
                id: delegate
                title: Delegate
                fields:
                  - name: note
                    type: text
                    label: Note
                """;
        Class<? extends ModalModel> clazz = loader.load(yaml);
        ModalModel instance = clazz.getDeclaredConstructor().newInstance();
        set(instance, "note", "hello");

        AtomicReference<Map<String, Object>> seenValues = new AtomicReference<>();
        AtomicReference<ModalInteractionEvent> seenEvent = new AtomicReference<>();

        ModalHandler<ModalModel> handler = new YamlModalHandler(clazz, (event, values) -> {
            seenEvent.set(event);
            seenValues.set(values);
        });

        ModalInteractionEvent dummyEvent = null;
        handler.handle(dummyEvent, instance);

        assertNotNull(seenValues.get());
        assertEquals("hello", seenValues.get().get("note"));
        assertSame(dummyEvent, seenEvent.get());
    }

    @Test
    void get_modal_class_returns_loaded_class() {
        String yaml = """
                id: x
                title: X
                fields:
                  - name: f
                    type: text
                    label: F
                """;
        Class<? extends ModalModel> clazz = loader.load(yaml);
        YamlModalHandler handler = new YamlModalHandler(clazz, (e, v) -> {});

        assertSame(clazz, handler.getModalClass());
        assertNull(handler.getStepResolver());
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
