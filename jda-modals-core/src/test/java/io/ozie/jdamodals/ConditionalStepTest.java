package io.ozie.jdamodals;

import io.ozie.jdamodals.annotation.ConditionalStep;
import io.ozie.jdamodals.annotation.Modal;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.SelectOption;
import io.ozie.jdamodals.annotation.TextInput;
import io.ozie.jdamodals.session.ModalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalStepTest {

    private ModalMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ModalMapper();
    }

    // -- Test Models --

    @Modal(id = "survey", title = "Survey")
    public static class SurveyModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.STRING,
                label = "Do you have experience?",
                step = 1,
                options = {
                        @SelectOption(label = "Yes", value = "yes"),
                        @SelectOption(label = "No", value = "no")
                }
        )
        private String hasExperience;

        @TextInput(label = "Describe your experience", step = 2)
        @ConditionalStep(dependsOn = "hasExperience", havingValue = "yes")
        private String experienceDetails;

        @TextInput(label = "Your email", step = 3)
        private String email;

        public String getHasExperience() { return hasExperience; }
        public String getExperienceDetails() { return experienceDetails; }
        public String getEmail() { return email; }
    }

    @Modal(id = "negated", title = "Negated Condition")
    public static class NegatedConditionModal extends ModalModel {
        @TextInput(label = "Are you a student?", step = 1)
        private String isStudent;

        @TextInput(label = "Company name", step = 2)
        @ConditionalStep(dependsOn = "isStudent", havingValue = "yes", negate = true)
        private String companyName;

        @TextInput(label = "Contact info", step = 3)
        private String contact;

        public String getIsStudent() { return isStudent; }
        public String getCompanyName() { return companyName; }
        public String getContact() { return contact; }
    }

    @Modal(id = "multi-value", title = "Multiple Values")
    public static class MultiValueConditionModal extends ModalModel {
        @SelectMenu(
                type = SelectMenuType.STRING,
                label = "Role",
                step = 1,
                options = {
                        @SelectOption(label = "Admin", value = "admin"),
                        @SelectOption(label = "Moderator", value = "mod"),
                        @SelectOption(label = "User", value = "user")
                }
        )
        private String role;

        @TextInput(label = "Admin panel URL", step = 2)
        @ConditionalStep(dependsOn = "role", havingValue = {"admin", "mod"})
        private String adminUrl;

        @TextInput(label = "Bio", step = 3)
        private String bio;

        public String getRole() { return role; }
        public String getAdminUrl() { return adminUrl; }
        public String getBio() { return bio; }
    }

    @Modal(id = "consecutive-skip", title = "Consecutive Skip")
    public static class ConsecutiveSkipModal extends ModalModel {
        @TextInput(label = "Type", step = 1)
        private String type;

        @TextInput(label = "Detail A", step = 2)
        @ConditionalStep(dependsOn = "type", havingValue = "a")
        private String detailA;

        @TextInput(label = "Detail B", step = 3)
        @ConditionalStep(dependsOn = "type", havingValue = "b")
        private String detailB;

        @TextInput(label = "Summary", step = 4)
        private String summary;

        public String getType() { return type; }
        public String getDetailA() { return detailA; }
        public String getDetailB() { return detailB; }
        public String getSummary() { return summary; }
    }

    // -- Tests --

    @Nested
    @DisplayName("Annotation-based @ConditionalStep")
    class AnnotationBased {

        @Test
        @DisplayName("should show conditional step when condition is met")
        void showStepWhenConditionMet() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            session.setFieldValue("hasExperience", "yes");

            int nextStep = mapper.resolveNextStep(session, 1, 3);
            assertEquals(2, nextStep);
        }

        @Test
        @DisplayName("should skip conditional step when condition is not met")
        void skipStepWhenConditionNotMet() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            session.setFieldValue("hasExperience", "no");

            int nextStep = mapper.resolveNextStep(session, 1, 3);
            assertEquals(3, nextStep);
        }

        @Test
        @DisplayName("should proceed normally from unconditional step")
        void proceedNormallyFromUnconditionalStep() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            session.setFieldValue("hasExperience", "no");
            session.setFieldValue("email", "test@test.com");

            // From step 3 (last step), next should be > totalSteps
            int nextStep = mapper.resolveNextStep(session, 3, 3);
            assertTrue(nextStep > 3);
        }

        @Test
        @DisplayName("should handle negate = true (show when value does NOT match)")
        void handleNegatedCondition() {
            ModalSession session = new ModalSession("user1", NegatedConditionModal.class, 3);

            // isStudent = "yes" with negate=true means skip step 2
            session.setFieldValue("isStudent", "yes");
            assertEquals(3, mapper.resolveNextStep(session, 1, 3));

            // isStudent = "no" with negate=true means show step 2
            session.setFieldValue("isStudent", "no");
            assertEquals(2, mapper.resolveNextStep(session, 1, 3));
        }

        @Test
        @DisplayName("should match any of multiple havingValue values")
        void matchMultipleHavingValues() {
            ModalSession session = new ModalSession("user1", MultiValueConditionModal.class, 3);

            session.setFieldValue("role", "admin");
            assertEquals(2, mapper.resolveNextStep(session, 1, 3));

            session.setFieldValue("role", "mod");
            assertEquals(2, mapper.resolveNextStep(session, 1, 3));

            session.setFieldValue("role", "user");
            assertEquals(3, mapper.resolveNextStep(session, 1, 3));
        }

        @Test
        @DisplayName("should skip multiple consecutive conditional steps")
        void skipConsecutiveConditionalSteps() {
            ModalSession session = new ModalSession("user1", ConsecutiveSkipModal.class, 4);

            // type = "c" doesn't match either step 2 ("a") or step 3 ("b")
            session.setFieldValue("type", "c");
            assertEquals(4, mapper.resolveNextStep(session, 1, 4));
        }

        @Test
        @DisplayName("should show only the matching branch step")
        void showOnlyMatchingBranch() {
            ModalSession session = new ModalSession("user1", ConsecutiveSkipModal.class, 4);

            session.setFieldValue("type", "a");
            assertEquals(2, mapper.resolveNextStep(session, 1, 4));

            // After step 2 (detailA), should skip step 3 (detailB requires "b") and go to 4
            assertEquals(4, mapper.resolveNextStep(session, 2, 4));
        }

        @Test
        @DisplayName("should finalize when all remaining steps are conditional and skipped")
        void finalizeWhenAllRemainingSkipped() {
            ModalSession session = new ModalSession("user1", ConsecutiveSkipModal.class, 4);
            session.setFieldValue("type", "c");
            session.setFieldValue("summary", "done");

            // From step 4 (last), next should be > totalSteps
            int next = mapper.resolveNextStep(session, 4, 4);
            assertTrue(next > 4);
        }

        @Test
        @DisplayName("should handle null field value in condition")
        void handleNullFieldValue() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            // hasExperience not set (null) - condition "yes" not met, skip step 2

            int nextStep = mapper.resolveNextStep(session, 1, 3);
            assertEquals(3, nextStep);
        }
    }

    @Nested
    @DisplayName("isStepConditionMet")
    class StepConditionCheck {

        @Test
        @DisplayName("unconditional step is always met")
        void unconditionalStepAlwaysMet() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            assertTrue(mapper.isStepConditionMet(SurveyModal.class, 1, session));
            assertTrue(mapper.isStepConditionMet(SurveyModal.class, 3, session));
        }

        @Test
        @DisplayName("conditional step reflects session values")
        void conditionalStepReflectsSession() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);

            session.setFieldValue("hasExperience", "no");
            assertFalse(mapper.isStepConditionMet(SurveyModal.class, 2, session));

            session.setFieldValue("hasExperience", "yes");
            assertTrue(mapper.isStepConditionMet(SurveyModal.class, 2, session));
        }
    }

    @Nested
    @DisplayName("hasConditionalSteps detection")
    class ConditionalStepsDetection {

        @Test
        @DisplayName("should detect classes with conditional steps")
        void detectConditionalSteps() {
            assertTrue(mapper.hasConditionalSteps(SurveyModal.class));
        }

        @Test
        @DisplayName("should return false for classes without conditional steps")
        void noConditionalSteps() {
            assertFalse(mapper.hasConditionalSteps(NoConditionModal.class));
        }
    }

    @Modal(id = "no-condition", title = "No Condition")
    public static class NoConditionModal extends ModalModel {
        @TextInput(label = "Name", step = 1)
        private String name;
        @TextInput(label = "Email", step = 2)
        private String email;
    }

    @Nested
    @DisplayName("StepResolver (programmatic)")
    class StepResolverTest {

        @Test
        @DisplayName("StepResolver overrides annotation-based resolution")
        void resolverOverridesAnnotations() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            session.setFieldValue("hasExperience", "yes");

            // Annotation would say go to step 2, but resolver says skip to 3
            StepResolver resolver = (s, currentStep, totalSteps) -> {
                if (currentStep == 1) return 3;
                return currentStep + 1;
            };

            int next = resolver.resolveNextStep(session, 1, 3);
            assertEquals(3, next);
        }

        @Test
        @DisplayName("StepResolver can finalize early")
        void resolverCanFinalizeEarly() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);

            StepResolver resolver = (s, currentStep, totalSteps) -> totalSteps + 1;

            int next = resolver.resolveNextStep(session, 1, 3);
            assertTrue(next > 3);
        }

        @Test
        @DisplayName("StepResolver can use session data for complex branching")
        void resolverUsesSessionData() {
            ModalSession session = new ModalSession("user1", ConsecutiveSkipModal.class, 4);
            session.setFieldValue("type", "a");

            StepResolver resolver = (s, currentStep, totalSteps) -> {
                if (currentStep == 1) {
                    String type = (String) s.getFieldValue("type");
                    return switch (type) {
                        case "a" -> 2;
                        case "b" -> 3;
                        default -> 4;
                    };
                }
                return 4; // always go to summary after detail step
            };

            assertEquals(2, resolver.resolveNextStep(session, 1, 4));

            session.setFieldValue("type", "b");
            assertEquals(3, resolver.resolveNextStep(session, 1, 4));

            session.setFieldValue("type", "c");
            assertEquals(4, resolver.resolveNextStep(session, 1, 4));
        }
    }

    @Nested
    @DisplayName("ModalSession setCurrentStep")
    class SessionStepJump {

        @Test
        @DisplayName("setCurrentStep allows jumping to arbitrary step")
        void jumpToArbitraryStep() {
            ModalSession session = new ModalSession("user1", SurveyModal.class, 3);
            assertEquals(1, session.getCurrentStep());

            session.setCurrentStep(3);
            assertEquals(3, session.getCurrentStep());

            session.setCurrentStep(2);
            assertEquals(2, session.getCurrentStep());
        }
    }
}
