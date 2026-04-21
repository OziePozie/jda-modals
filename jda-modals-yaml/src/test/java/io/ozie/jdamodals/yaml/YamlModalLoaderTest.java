package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalMapper;
import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.annotation.ConditionalStep;
import io.ozie.jdamodals.annotation.Modal;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.TextInput;
import io.ozie.jdamodals.session.ModalSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlModalLoaderTest {

    private final YamlModalLoader loader = new YamlModalLoader();

    @Test
    void loads_simple_text_modal() {
        String yaml = """
                id: registration
                title: Registration
                fields:
                  - name: fullName
                    type: text
                    label: Full name
                    placeholder: John Doe
                    required: true
                  - name: email
                    type: text
                    label: Email
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);

        Modal modal = clazz.getAnnotation(Modal.class);
        assertNotNull(modal);
        assertEquals("registration", modal.id());
        assertEquals("Registration", modal.title());

        Field fullName = findField(clazz, "fullName");
        assertEquals(String.class, fullName.getType());
        TextInput ti = fullName.getAnnotation(TextInput.class);
        assertNotNull(ti);
        assertEquals("Full name", ti.label());
        assertEquals("John Doe", ti.placeholder());
        assertTrue(ti.required());

        assertNotNull(findField(clazz, "email").getAnnotation(TextInput.class));
    }

    @Test
    void loads_string_select_with_options() {
        String yaml = """
                id: role_picker
                title: Pick a role
                fields:
                  - name: role
                    type: select
                    menuType: STRING
                    label: Role
                    options:
                      - value: admin
                        label: Administrator
                      - value: user
                        label: Regular user
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        Field role = findField(clazz, "role");
        SelectMenu sm = role.getAnnotation(SelectMenu.class);

        assertNotNull(sm);
        assertEquals(SelectMenuType.STRING, sm.type());
        assertEquals(2, sm.options().length);
        assertEquals("admin", sm.options()[0].value());
        assertEquals("Administrator", sm.options()[0].label());
        assertEquals(String.class, role.getType());
    }

    @Test
    void integrates_with_modal_mapper() {
        String yaml = """
                id: feedback
                title: Feedback
                fields:
                  - name: subject
                    type: text
                    label: Subject
                  - name: body
                    type: text
                    label: Body
                    style: PARAGRAPH
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        var jdaModal = new ModalMapper().build(clazz);

        assertEquals("feedback", jdaModal.getId());
        assertEquals("Feedback", jdaModal.getTitle());
        assertEquals(2, jdaModal.getComponents().size());
    }

    @Test
    void can_instantiate_generated_class() throws Exception {
        String yaml = """
                id: simple
                title: Simple
                fields:
                  - name: note
                    type: text
                    label: Note
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        ModalModel instance = clazz.getDeclaredConstructor().newInstance();

        assertEquals("simple", instance.getModalId());
        assertEquals("Simple", instance.getModalTitle());
    }

    @Test
    void conditional_step_is_attached_from_when_block() {
        String yaml = """
                id: survey
                title: Survey
                fields:
                  - name: hasExperience
                    type: select
                    menuType: STRING
                    label: Do you have experience?
                    step: 1
                    options:
                      - { value: "yes", label: "Yes" }
                      - { value: "no",  label: "No"  }

                  - name: experienceDetails
                    type: text
                    label: Describe your experience
                    style: PARAGRAPH
                    step: 2
                    when:
                      dependsOn: hasExperience
                      havingValue: ["yes", "maybe"]

                  - name: email
                    type: text
                    label: Your email
                    step: 3
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);

        ConditionalStep cond = findField(clazz, "experienceDetails").getAnnotation(ConditionalStep.class);
        assertNotNull(cond);
        assertEquals("hasExperience", cond.dependsOn());
        assertArrayEquals(new String[]{"yes", "maybe"}, cond.havingValue());
        assertFalse(cond.negate());

        assertNull(findField(clazz, "hasExperience").getAnnotation(ConditionalStep.class));
        assertNull(findField(clazz, "email").getAnnotation(ConditionalStep.class));
    }

    @Test
    void conditional_step_branching_runs_end_to_end() {
        String yaml = """
                id: survey
                title: Survey
                fields:
                  - name: hasExperience
                    type: select
                    menuType: STRING
                    label: Do you have experience?
                    step: 1
                    options:
                      - { value: "yes", label: "Yes" }
                      - { value: "no",  label: "No"  }

                  - name: experienceDetails
                    type: text
                    label: Describe your experience
                    step: 2
                    when:
                      dependsOn: hasExperience
                      havingValue: "yes"

                  - name: email
                    type: text
                    label: Your email
                    step: 3
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        ModalMapper mapper = new ModalMapper();
        assertEquals(3, mapper.getTotalSteps(clazz));
        assertTrue(mapper.hasConditionalSteps(clazz));

        ModalSession sessionYes = new ModalSession("u", clazz, 3);
        sessionYes.setFieldValue("hasExperience", "yes");
        assertEquals(2, mapper.resolveNextStep(sessionYes, 1, 3),
                "step 2 should run when hasExperience=yes");

        ModalSession sessionNo = new ModalSession("u", clazz, 3);
        sessionNo.setFieldValue("hasExperience", "no");
        assertEquals(3, mapper.resolveNextStep(sessionNo, 1, 3),
                "step 2 should be skipped when hasExperience=no");
    }

    @Test
    void negate_flag_inverts_condition() {
        String yaml = """
                id: negated
                title: Negated
                fields:
                  - name: isStudent
                    type: text
                    label: Are you a student?
                    step: 1
                  - name: companyName
                    type: text
                    label: Company name
                    step: 2
                    when:
                      dependsOn: isStudent
                      havingValue: "yes"
                      negate: true
                """;

        Class<? extends ModalModel> clazz = loader.load(yaml);
        ConditionalStep cond = findField(clazz, "companyName").getAnnotation(ConditionalStep.class);
        assertNotNull(cond);
        assertTrue(cond.negate());
    }

    private static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field not found: " + name, e);
        }
    }
}
