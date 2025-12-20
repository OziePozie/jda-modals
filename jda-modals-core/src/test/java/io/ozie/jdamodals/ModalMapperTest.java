package io.ozie.jdamodals;

import io.ozie.jdamodals.annotation.Modal;
import io.ozie.jdamodals.annotation.NestedModal;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.SelectOption;
import io.ozie.jdamodals.annotation.TextInput;
import io.ozie.jdamodals.exception.ModalMappingException;
import io.ozie.jdamodals.session.ModalSession;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModalMapperTest {

    private ModalMapper mapper;

    @Mock
    private ModalInteractionEvent event;

    @BeforeEach
    void setUp() {
        mapper = new ModalMapper();
    }

    @Modal(id = "simple", title = "Simple Modal")
    public static class SimpleModal extends ModalModel {
        @TextInput(label = "Name")
        private String name;

        @TextInput(label = "Email")
        private String email;

        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    @Modal(id = "custom-ids", title = "Custom IDs Modal")
    public static class CustomIdModal extends ModalModel {
        @TextInput(id = "user_name", label = "Name")
        private String name;

        @TextInput(id = "user_email", label = "Email")
        private String email;

        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    @Modal(id = "numeric", title = "Numeric Modal")
    public static class NumericModal extends ModalModel {
        @TextInput(label = "Age")
        private Integer age;

        @TextInput(label = "Score")
        private int score;

        @TextInput(label = "Price")
        private Double price;

        @TextInput(label = "Count")
        private Long count;

        public Integer getAge() { return age; }
        public int getScore() { return score; }
        public Double getPrice() { return price; }
        public Long getCount() { return count; }
    }

    @Modal(id = "wizard", title = "Wizard Modal")
    public static class WizardModal extends ModalModel {
        @TextInput(label = "First Name", step = 1, order = 1)
        private String firstName;

        @TextInput(label = "Last Name", step = 1, order = 2)
        private String lastName;

        @TextInput(label = "Email", step = 2)
        private String email;

        @TextInput(label = "Phone", step = 3)
        private String phone;

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }

    public static class AddressModel extends ModalModel {
        @TextInput(label = "Street", step = 2)
        private String street;

        @TextInput(label = "City", step = 2)
        private String city;

        public String getStreet() { return street; }
        public String getCity() { return city; }
    }

    @Modal(id = "nested", title = "Nested Modal")
    public static class NestedModalModel extends ModalModel {
        @TextInput(label = "Name", step = 1)
        private String name;

        @NestedModal(prefix = "addr", order = 2, titleSuffix = " - Address")
        private AddressModel address;

        public String getName() { return name; }
        public AddressModel getAddress() { return address; }
    }

    @Modal(id = "optional", title = "Optional Fields Modal")
    public static class OptionalFieldsModal extends ModalModel {
        @TextInput(label = "Required Field", required = true)
        private String requiredField;

        @TextInput(label = "Optional Field", required = false)
        private String optionalField;

        public String getRequiredField() { return requiredField; }
        public String getOptionalField() { return optionalField; }
    }

    @Modal(id = "six-fields-modal-in-one-step", title = "Six fields Fields Modal")
    public static class SixFieldsModal extends ModalModel {
        @TextInput(label = "1 Field", required = true)
        private String firstField;
        @TextInput(label = "2 Field", required = true)
        private String secondField;
        @TextInput(label = "3 Field", required = true)
        private String thirdField;
        @TextInput(label = "4 Field", required = true)
        private String fourthField;
        @TextInput(label = "5 Field", required = true)
        private String fifthField;
        @TextInput(label = "6 Field", required = true)
        private String sixField;

        public String getFirstField() {
            return firstField;
        }

        public String getSecondField() {
            return secondField;
        }

        public String getThirdField() {
            return thirdField;
        }

        public String getFourthField() {
            return fourthField;
        }

        public String getFifthField() {
            return fifthField;
        }

        public String getSixField() {
            return sixField;
        }
    }


    @Nested
    @DisplayName("String Field Mapping")
    class StringFieldMapping {

        @Test
        @DisplayName("should map string fields using field names as IDs")
        void mapStringFieldsWithDefaultIds() {
            ModalMapping nameMapping = mockMapping("John");
            ModalMapping emailMapping = mockMapping("john@example.com");

            when(event.getValue("name")).thenReturn(nameMapping);
            when(event.getValue("email")).thenReturn(emailMapping);

            SimpleModal result = mapper.map(event, SimpleModal.class);

            assertEquals("John", result.getName());
            assertEquals("john@example.com", result.getEmail());
        }

        @Test
        @DisplayName("should map string fields using custom IDs")
        void mapStringFieldsWithCustomIds() {
            ModalMapping nameMapping = mockMapping("Jane");
            ModalMapping emailMapping = mockMapping("jane@example.com");

            when(event.getValue("user_name")).thenReturn(nameMapping);
            when(event.getValue("user_email")).thenReturn(emailMapping);

            CustomIdModal result = mapper.map(event, CustomIdModal.class);

            assertEquals("Jane", result.getName());
            assertEquals("jane@example.com", result.getEmail());
        }

        @Test
        @DisplayName("should handle null values for optional fields")
        void handleNullValues() {
            ModalMapping requiredMapping = mockMapping("Required Value");

            when(event.getValue("requiredField")).thenReturn(requiredMapping);
            when(event.getValue("optionalField")).thenReturn(null);

            OptionalFieldsModal result = mapper.map(event, OptionalFieldsModal.class);

            assertEquals("Required Value", result.getRequiredField());
            assertNull(result.getOptionalField());
        }

        @Test
        @DisplayName("should handle empty string values")
        void handleEmptyStrings() {
            ModalMapping nameMapping = mockMapping("");
            ModalMapping emailMapping = mockMapping("test@test.com");

            when(event.getValue("name")).thenReturn(nameMapping);
            when(event.getValue("email")).thenReturn(emailMapping);

            SimpleModal result = mapper.map(event, SimpleModal.class);

            assertEquals("", result.getName());
            assertEquals("test@test.com", result.getEmail());
        }
    }

    @Nested
    @DisplayName("Numeric Field Mapping")
    class NumericFieldMapping {

        @Test
        @DisplayName("should map Integer fields")
        void mapIntegerField() {
            ModalMapping ageMapping = mockMapping("25");
            ModalMapping scoreMapping = mockMapping("100");
            ModalMapping priceMapping = mockMapping("19.99");
            ModalMapping countMapping = mockMapping("1000000");

            when(event.getValue("age")).thenReturn(ageMapping);
            when(event.getValue("score")).thenReturn(scoreMapping);
            when(event.getValue("price")).thenReturn(priceMapping);
            when(event.getValue("count")).thenReturn(countMapping);

            NumericModal result = mapper.map(event, NumericModal.class);

            assertEquals(25, result.getAge());
            assertEquals(100, result.getScore());
            assertEquals(19.99, result.getPrice());
            assertEquals(1000000L, result.getCount());
        }

        @Test
        @DisplayName("should handle empty numeric values as null for wrapper types")
        void handleEmptyNumericValues() {
            ModalMapping ageMapping = mockMapping("");
            ModalMapping scoreMapping = mockMapping("50");
            ModalMapping priceMapping = mockMapping("");
            ModalMapping countMapping = mockMapping("");

            when(event.getValue("age")).thenReturn(ageMapping);
            when(event.getValue("score")).thenReturn(scoreMapping);
            when(event.getValue("price")).thenReturn(priceMapping);
            when(event.getValue("count")).thenReturn(countMapping);

            NumericModal result = mapper.map(event, NumericModal.class);

            assertNull(result.getAge());
            assertEquals(50, result.getScore());
            assertNull(result.getPrice());
            assertNull(result.getCount());
        }

        @Test
        @DisplayName("should throw exception for invalid numeric format")
        void throwExceptionForInvalidNumericFormat() {
            ModalMapping ageMapping = mockMapping("not-a-number");

            when(event.getValue("age")).thenReturn(ageMapping);

            assertThrows(NumberFormatException.class, () -> mapper.map(event, NumericModal.class));
        }
    }

    @Nested
    @DisplayName("Multi-Step Modal Mapping")
    class MultiStepMapping {

        @Test
        @DisplayName("should detect total steps correctly")
        void detectTotalSteps() {
            assertEquals(3, mapper.getTotalSteps(WizardModal.class));
            assertEquals(1, mapper.getTotalSteps(SimpleModal.class));
        }

        @Test
        @DisplayName("should identify multi-step modals")
        void identifyMultiStepModals() {
            assertTrue(mapper.isMultiStep(WizardModal.class));
            assertFalse(mapper.isMultiStep(SimpleModal.class));
        }

        @Test
        @DisplayName("should count fields per step correctly")
        void countFieldsPerStep() {
            assertEquals(2, mapper.getFieldCountForStep(WizardModal.class, 1));
            assertEquals(1, mapper.getFieldCountForStep(WizardModal.class, 2));
            assertEquals(1, mapper.getFieldCountForStep(WizardModal.class, 3));
        }

        @Test
        @DisplayName("should map step data to session")
        void mapStepDataToSession() {
            ModalSession session = new ModalSession("user123", WizardModal.class, 3);

            ModalMapping firstNameMapping = mockMapping("John");
            ModalMapping lastNameMapping = mockMapping("Doe");

            when(event.getValue("firstName")).thenReturn(firstNameMapping);
            when(event.getValue("lastName")).thenReturn(lastNameMapping);

            mapper.mapStepToSession(event, session, 1);

            assertEquals("John", session.getFieldValue("firstName"));
            assertEquals("Doe", session.getFieldValue("lastName"));
        }

        @Test
        @DisplayName("should map complete session to modal model")
        void mapSessionToModalModel() {
            ModalSession session = new ModalSession("user123", WizardModal.class, 3);
            session.setFieldValue("firstName", "John");
            session.setFieldValue("lastName", "Doe");
            session.setFieldValue("email", "john@example.com");
            session.setFieldValue("phone", "+1234567890");

            WizardModal result = mapper.mapFromSession(session);

            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
            assertEquals("john@example.com", result.getEmail());
            assertEquals("+1234567890", result.getPhone());
        }
    }

    @Nested
    @DisplayName("Nested Modal Mapping")
    class NestedModalMapping {

        @Test
        @DisplayName("should map nested modal fields with prefix")
        void mapNestedModalFieldsWithPrefix() {
            ModalMapping nameMapping = mockMapping("John Doe");
            ModalMapping streetMapping = mockMapping("123 Main St");
            ModalMapping cityMapping = mockMapping("New York");

            when(event.getValue("name")).thenReturn(nameMapping);
            when(event.getValue("addr_street")).thenReturn(streetMapping);
            when(event.getValue("addr_city")).thenReturn(cityMapping);

            NestedModalModel result = mapper.map(event, NestedModalModel.class);

            assertEquals("John Doe", result.getName());
            assertNotNull(result.getAddress());
            assertEquals("123 Main St", result.getAddress().getStreet());
            assertEquals("New York", result.getAddress().getCity());
        }

        @Test
        @DisplayName("should calculate total steps including nested modals")
        void calculateTotalStepsWithNestedModals() {
            assertEquals(2, mapper.getTotalSteps(NestedModalModel.class));
        }

        @Test
        @DisplayName("should map nested modal from session")
        void mapNestedModalFromSession() {
            ModalSession session = new ModalSession("user123", NestedModalModel.class, 2);
            session.setFieldValue("name", "Jane Doe");
            session.setFieldValue("addr_street", "456 Oak Ave");
            session.setFieldValue("addr_city", "Los Angeles");

            NestedModalModel result = mapper.mapFromSession(session);

            assertEquals("Jane Doe", result.getName());
            assertNotNull(result.getAddress());
            assertEquals("456 Oak Ave", result.getAddress().getStreet());
            assertEquals("Los Angeles", result.getAddress().getCity());
        }
    }

    @Nested
    @DisplayName("Modal ID Handling")
    class ModalIdHandling {

        @Test
        @DisplayName("should get modal ID from class")
        void getModalIdFromClass() {
            assertEquals("simple", mapper.getModalId(SimpleModal.class));
            assertEquals("wizard", mapper.getModalId(WizardModal.class));
        }

        @Test
        @DisplayName("should extract base modal ID from step ID")
        void extractBaseModalId() {
            assertEquals("wizard", mapper.getBaseModalId("wizard_step_1"));
            assertEquals("wizard", mapper.getBaseModalId("wizard_step_2"));
            assertEquals("simple", mapper.getBaseModalId("simple"));
        }

        @Test
        @DisplayName("should extract step number from modal ID")
        void extractStepNumber() {
            assertEquals(1, mapper.getStepFromModalId("wizard_step_1"));
            assertEquals(2, mapper.getStepFromModalId("wizard_step_2"));
            assertEquals(3, mapper.getStepFromModalId("wizard_step_3"));
            assertEquals(1, mapper.getStepFromModalId("simple"));
        }
    }

    @Nested
    @DisplayName("Modal Building")
    class ModalBuilding {

        @Test
        @DisplayName("should build single-step modal")
        void buildSingleStepModal() {
            var modal = mapper.build(SimpleModal.class);

            assertEquals("simple", modal.getId());
            assertEquals("Simple Modal", modal.getTitle());
            assertEquals(2, modal.getComponents().size());
        }

        @Test
        @DisplayName("should throw exception when modal has more than 5 components")
        void throwExceptionForTooManyComponents() {
            var exception = assertThrows(ModalMappingException.class,
                    () -> mapper.build(SixFieldsModal.class));

            assertTrue(exception.getMessage().contains("6 components"));
            assertTrue(exception.getMessage().contains("maximum 5"));
        }

        @Test
        @DisplayName("should build multi-step modal with step indicator")
        void buildMultiStepModalWithStepIndicator() {
            var step1 = mapper.buildForStep(WizardModal.class, 1);
            var step2 = mapper.buildForStep(WizardModal.class, 2);

            assertEquals("wizard_step_1", step1.getId());
            assertEquals("Wizard Modal (1/3)", step1.getTitle());
            assertEquals(2, step1.getComponents().size());

            assertEquals("wizard_step_2", step2.getId());
            assertEquals("Wizard Modal (2/3)", step2.getTitle());
            assertEquals(1, step2.getComponents().size());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        public static class NoAnnotationModal extends ModalModel {
            @TextInput(label = "Field")
            private String field;
        }

        @Test
        @DisplayName("should throw exception for class without @Modal annotation")
        void throwExceptionForMissingAnnotation() {
            assertThrows(ModalMappingException.class, () -> mapper.build(NoAnnotationModal.class));
        }
    }

    private ModalMapping mockMapping(String value) {
        ModalMapping mapping = mock(ModalMapping.class);
        when(mapping.getAsString()).thenReturn(value);
        return mapping;
    }

    // SelectMenu test models

    @Modal(id = "string-select", title = "String Select Modal")
    public static class StringSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.STRING,
                label = "Difficulty",
                options = {
                        @SelectOption(value = "easy", label = "Easy"),
                        @SelectOption(value = "medium", label = "Medium"),
                        @SelectOption(value = "hard", label = "Hard")
                }
        )
        private String difficulty;

        public String getDifficulty() { return difficulty; }
    }

    @Modal(id = "string-select-multi", title = "String Multi-Select Modal")
    public static class StringMultiSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.STRING,
                label = "Tags",
                maxValues = 3,
                options = {
                        @SelectOption(value = "java", label = "Java"),
                        @SelectOption(value = "kotlin", label = "Kotlin"),
                        @SelectOption(value = "python", label = "Python")
                }
        )
        private List<String> tags;

        public List<String> getTags() { return tags; }
    }

    @Modal(id = "user-select", title = "User Select Modal")
    public static class UserSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.USER,
                label = "Select User"
        )
        private User selectedUser;

        public User getSelectedUser() { return selectedUser; }
    }

    @Modal(id = "role-select", title = "Role Select Modal")
    public static class RoleSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.ROLE,
                label = "Select Roles",
                maxValues = 5
        )
        private List<Role> roles;

        public List<Role> getRoles() { return roles; }
    }

    @Modal(id = "channel-select", title = "Channel Select Modal")
    public static class ChannelSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.CHANNEL,
                label = "Select Channel"
        )
        private GuildChannel channel;

        public GuildChannel getChannel() { return channel; }
    }

    @Modal(id = "mentionable-select", title = "Mentionable Select Modal")
    public static class MentionableSelectModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.MENTIONABLE,
                label = "Select Mentionable",
                maxValues = 10
        )
        private List<IMentionable> mentionables;

        public List<IMentionable> getMentionables() { return mentionables; }
    }

    @Modal(id = "mixed-modal", title = "Mixed Modal")
    public static class MixedModal extends ModalModel {
        @TextInput(label = "Name", order = 1)
        private String name;

        @SelectMenu(
                type = SelectMenuType.STRING,
                label = "Color",
                order = 2,
                options = {
                        @SelectOption(value = "red", label = "Red"),
                        @SelectOption(value = "blue", label = "Blue")
                }
        )
        private String color;

        public String getName() { return name; }
        public String getColor() { return color; }
    }

    @Nested
    @DisplayName("SelectMenu String Mapping")
    class SelectMenuStringMapping {

        @Test
        @DisplayName("should map single string select value")
        void mapSingleStringSelectValue() {
            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsStringList()).thenReturn(List.of("hard"));
            when(event.getValue("difficulty")).thenReturn(mapping);

            StringSelectModal result = mapper.map(event, StringSelectModal.class);

            assertEquals("hard", result.getDifficulty());
        }

        @Test
        @DisplayName("should map multiple string select values")
        void mapMultipleStringSelectValues() {
            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsStringList()).thenReturn(List.of("java", "kotlin"));
            when(event.getValue("tags")).thenReturn(mapping);

            StringMultiSelectModal result = mapper.map(event, StringMultiSelectModal.class);

            assertEquals(2, result.getTags().size());
            assertTrue(result.getTags().contains("java"));
            assertTrue(result.getTags().contains("kotlin"));
        }

        @Test
        @DisplayName("should handle empty string select")
        void handleEmptyStringSelect() {
            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsStringList()).thenReturn(List.of());
            when(event.getValue("difficulty")).thenReturn(mapping);

            StringSelectModal result = mapper.map(event, StringSelectModal.class);

            assertNull(result.getDifficulty());
        }
    }

    @Nested
    @DisplayName("SelectMenu Entity Mapping")
    class SelectMenuEntityMapping {

        @Test
        @DisplayName("should map user select value")
        void mapUserSelectValue() {
            User mockUser = mock(User.class);
            Mentions mentions = mock(Mentions.class);
            when(mentions.getUsers()).thenReturn(List.of(mockUser));

            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsMentions()).thenReturn(mentions);
            when(event.getValue("selectedUser")).thenReturn(mapping);

            UserSelectModal result = mapper.map(event, UserSelectModal.class);

            assertEquals(mockUser, result.getSelectedUser());
        }

        @Test
        @DisplayName("should map role select values")
        void mapRoleSelectValues() {
            Role role1 = mock(Role.class);
            Role role2 = mock(Role.class);
            Mentions mentions = mock(Mentions.class);
            when(mentions.getRoles()).thenReturn(List.of(role1, role2));

            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsMentions()).thenReturn(mentions);
            when(event.getValue("roles")).thenReturn(mapping);

            RoleSelectModal result = mapper.map(event, RoleSelectModal.class);

            assertEquals(2, result.getRoles().size());
            assertTrue(result.getRoles().contains(role1));
            assertTrue(result.getRoles().contains(role2));
        }

        @Test
        @DisplayName("should map channel select value")
        void mapChannelSelectValue() {
            GuildChannel mockChannel = mock(GuildChannel.class);
            Mentions mentions = mock(Mentions.class);
            when(mentions.getChannels()).thenReturn(List.of(mockChannel));

            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsMentions()).thenReturn(mentions);
            when(event.getValue("channel")).thenReturn(mapping);

            ChannelSelectModal result = mapper.map(event, ChannelSelectModal.class);

            assertEquals(mockChannel, result.getChannel());
        }

        @Test
        @DisplayName("should map mentionable select values")
        void mapMentionableSelectValues() {
            User mockUser = mock(User.class);
            Role mockRole = mock(Role.class);
            Mentions mentions = mock(Mentions.class);
            when(mentions.getUsers()).thenReturn(List.of(mockUser));
            when(mentions.getRoles()).thenReturn(List.of(mockRole));

            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getAsMentions()).thenReturn(mentions);
            when(event.getValue("mentionables")).thenReturn(mapping);

            MentionableSelectModal result = mapper.map(event, MentionableSelectModal.class);

            assertEquals(2, result.getMentionables().size());
        }

        @Test
        @DisplayName("should handle null user select")
        void handleNullUserSelect() {
            when(event.getValue("selectedUser")).thenReturn(null);

            UserSelectModal result = mapper.map(event, UserSelectModal.class);

            assertNull(result.getSelectedUser());
        }
    }

    @Nested
    @DisplayName("SelectMenu Modal Building")
    class SelectMenuModalBuilding {

        @Test
        @DisplayName("should build modal with string select menu")
        void buildModalWithStringSelectMenu() {
            var modal = mapper.build(StringSelectModal.class);

            assertEquals("string-select", modal.getId());
            assertEquals(1, modal.getComponents().size());
        }

        @Test
        @DisplayName("should build mixed modal with text input and select menu")
        void buildMixedModal() {
            var modal = mapper.build(MixedModal.class);

            assertEquals("mixed-modal", modal.getId());
            assertEquals(2, modal.getComponents().size());
        }

        @Test
        @DisplayName("should include select menu in step count")
        void includeSelectMenuInStepCount() {
            assertEquals(1, mapper.getFieldCountForStep(StringSelectModal.class, 1));
            assertEquals(2, mapper.getFieldCountForStep(MixedModal.class, 1));
        }
    }
}
