package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalHandler;
import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.annotation.Attachment;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.TextInput;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlModalBindingTest {

    private final YamlModalLoader loader = new YamlModalLoader();

    public interface RegistrationForm {
        String fullName();
        Integer age();
        Message.Attachment avatar();
    }

    public interface TeamPicker {
        User mentor();
        List<User> teammates();
        String role();
    }

    public interface Simple {
        String note();
    }

    @Test
    void bind_generates_class_that_implements_interface() throws Exception {
        String yaml = """
                id: registration
                title: Registration
                fields:
                  - name: fullName
                    label: Full name
                  - name: age
                    label: Age
                  - name: avatar
                    label: Avatar
                    required: false
                """;

        Class<? extends ModalModel> clazz = loader.bind(yaml, RegistrationForm.class);

        assertTrue(RegistrationForm.class.isAssignableFrom(clazz));
        assertTrue(ModalModel.class.isAssignableFrom(clazz));
    }

    @Test
    void typed_getters_read_generated_field_values() throws Exception {
        String yaml = """
                id: registration
                title: Registration
                fields:
                  - name: fullName
                    label: Full name
                  - name: age
                    label: Age
                  - name: avatar
                    label: Avatar
                    required: false
                """;

        Class<? extends ModalModel> clazz = loader.bind(yaml, RegistrationForm.class);
        RegistrationForm form = (RegistrationForm) clazz.getDeclaredConstructor().newInstance();
        setField(form, "fullName", "Ada");
        setField(form, "age", 36);

        assertEquals("Ada", form.fullName());
        assertEquals(36, form.age());
        assertEquals(null, form.avatar());
    }

    @Test
    void field_types_and_annotations_inferred_from_interface() throws Exception {
        String yaml = """
                id: registration
                title: Registration
                fields:
                  - name: fullName
                    label: Full name
                  - name: age
                    label: Age
                  - name: avatar
                    label: Avatar
                """;

        Class<? extends ModalModel> clazz = loader.bind(yaml, RegistrationForm.class);

        Field fullName = clazz.getDeclaredField("fullName");
        assertEquals(String.class, fullName.getType());
        assertNotNull(fullName.getAnnotation(TextInput.class));

        Field age = clazz.getDeclaredField("age");
        assertEquals(Integer.class, age.getType());
        assertNotNull(age.getAnnotation(TextInput.class));

        Field avatar = clazz.getDeclaredField("avatar");
        assertEquals(Message.Attachment.class, avatar.getType());
        assertNotNull(avatar.getAnnotation(Attachment.class));
    }

    @Test
    void select_types_inferred_from_entity_return_types() throws Exception {
        String yaml = """
                id: team
                title: Team
                fields:
                  - name: mentor
                    label: Mentor
                  - name: teammates
                    label: Teammates
                  - name: role
                    label: Role
                    options:
                      - { value: admin, label: Admin }
                      - { value: dev,   label: Dev   }
                """;

        Class<? extends ModalModel> clazz = loader.bind(yaml, TeamPicker.class);

        SelectMenu mentor = clazz.getDeclaredField("mentor").getAnnotation(SelectMenu.class);
        assertEquals(SelectMenuType.USER, mentor.type());

        Field teammatesField = clazz.getDeclaredField("teammates");
        SelectMenu teammates = teammatesField.getAnnotation(SelectMenu.class);
        assertEquals(SelectMenuType.USER, teammates.type());
        assertEquals(List.class, teammatesField.getType());
        assertTrue(teammates.maxValues() > 1, "multi select should have maxValues > 1");

        SelectMenu role = clazz.getDeclaredField("role").getAnnotation(SelectMenu.class);
        assertEquals(SelectMenuType.STRING, role.type());
        assertEquals(2, role.options().length);
    }

    @Test
    void typed_handler_passes_view_to_consumer() throws Exception {
        String yaml = """
                id: simple
                title: Simple
                fields:
                  - name: note
                    label: Note
                """;

        Class<? extends ModalModel> clazz = loader.bind(yaml, Simple.class);
        ModalModel instance = clazz.getDeclaredConstructor().newInstance();
        setField(instance, "note", "hi");

        AtomicReference<Simple> seen = new AtomicReference<>();
        ModalHandler<ModalModel> handler = new TypedYamlModalHandler<>(
                clazz, Simple.class, (event, form) -> seen.set(form));

        handler.handle((ModalInteractionEvent) null, instance);

        assertNotNull(seen.get());
        assertEquals("hi", seen.get().note());
        assertInstanceOf(Simple.class, seen.get());
    }

    @Test
    void typed_handler_rejects_mismatched_class() {
        String yaml = """
                id: plain
                title: Plain
                fields:
                  - name: note
                    type: text
                    label: Note
                """;
        Class<? extends ModalModel> notBound = loader.load(yaml);

        assertThrows(IllegalArgumentException.class,
                () -> new TypedYamlModalHandler<>(notBound, Simple.class, (e, f) -> {}));
    }

    @Test
    void bind_rejects_non_interface() {
        String yaml = """
                id: x
                title: X
                fields:
                  - name: note
                    type: text
                    label: Note
                """;
        assertThrows(IllegalArgumentException.class,
                () -> loader.bind(yaml, String.class));
    }

    @Test
    void bind_rejects_yaml_field_without_matching_method() {
        String yaml = """
                id: mismatch
                title: Mismatch
                fields:
                  - name: nonexistent
                    label: Nope
                """;
        assertThrows(IllegalArgumentException.class,
                () -> loader.bind(yaml, Simple.class));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
