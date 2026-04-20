package io.ozie.jdamodals;

import io.ozie.jdamodals.annotation.Attachment;
import io.ozie.jdamodals.annotation.ConditionalStep;
import io.ozie.jdamodals.annotation.Modal;
import io.ozie.jdamodals.annotation.NestedModal;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.SelectMenuType;
import io.ozie.jdamodals.annotation.SelectOption;
import io.ozie.jdamodals.annotation.TextInput;
import io.ozie.jdamodals.exception.ModalMappingException;
import io.ozie.jdamodals.session.ModalSession;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Maps between ModalModel classes and JDA Modal objects.
 * Supports multi-step wizard modals with automatic step detection.
 * Supports nested modal models for composition and reuse.
 */
public class ModalMapper {

    private static final int MAX_COMPONENTS_PER_MODAL = 5;

    /**
     * Builds a JDA Modal from a ModalModel class (single-step or first step of wizard).
     */
    public <T extends ModalModel> net.dv8tion.jda.api.modals.Modal build(Class<T> clazz) {
        return buildForStep(clazz, 1);
    }

    /**
     * Builds a JDA Modal for a specific step of a wizard.
     *
     * @param clazz the model class
     * @param step  the step number (1-based)
     * @return built JDA Modal for the specified step
     */
    public <T extends ModalModel> net.dv8tion.jda.api.modals.Modal buildForStep(Class<T> clazz, int step) {
        Modal modalAnnotation = getModalAnnotation(clazz);
        int totalSteps = getTotalSteps(clazz);

        String titleSuffix = getTitleSuffixForStep(clazz, step);
        String baseTitle = modalAnnotation.title() + titleSuffix;

        String title = totalSteps > 1
                ? baseTitle + " (" + step + "/" + totalSteps + ")"
                : baseTitle;

        String modalId = totalSteps > 1
                ? modalAnnotation.id() + "_step_" + step
                : modalAnnotation.id();

        var builder = net.dv8tion.jda.api.modals.Modal.create(modalId, title);

        List<FieldComponent> components = collectComponentsForStep(clazz, step, "", 0);
        components.sort(Comparator.comparingInt(FieldComponent::order));

        if (components.size() > MAX_COMPONENTS_PER_MODAL) {
            throw new ModalMappingException(
                    "Modal '" + modalAnnotation.id() + "' step " + step + " has " + components.size() +
                    " components, but Discord allows maximum " + MAX_COMPONENTS_PER_MODAL +
                    ". Consider splitting into multiple steps.");
        }

        for (FieldComponent fc : components) {
            builder.addComponents(fc.labeled());
        }

        return builder.build();
    }

    /**
     * Maps a ModalInteractionEvent to a ModalModel instance (for single-step modals).
     */
    public <T extends ModalModel> T map(ModalInteractionEvent event, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            mapFieldsFromEvent(event, instance, clazz, "");
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new ModalMappingException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    /**
     * Maps fields from a ModalInteractionEvent to a session (for multi-step modals).
     * Only maps fields that belong to the current step.
     */
    public void mapStepToSession(ModalInteractionEvent event, ModalSession session, int step) {
        Class<?> clazz = session.getModalClass();
        mapStepToSessionRecursive(event, session, clazz, step, "", 0);
    }

    /**
     * Maps all values from a completed session to a ModalModel instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends ModalModel> T mapFromSession(ModalSession session) {
        try {
            Class<T> clazz = (Class<T>) session.getModalClass();
            return mapFromSessionRecursive(session, clazz, "");
        } catch (ReflectiveOperationException e) {
            throw new ModalMappingException("Failed to map session to model", e);
        }
    }

    /**
     * Gets the modal ID from a model class.
     */
    public String getModalId(Class<? extends ModalModel> clazz) {
        return getModalAnnotation(clazz).id();
    }

    /**
     * Gets the base modal ID (without step suffix) from an event modal ID.
     */
    public String getBaseModalId(String eventModalId) {
        int stepIndex = eventModalId.indexOf("_step_");
        return stepIndex > 0 ? eventModalId.substring(0, stepIndex) : eventModalId;
    }

    /**
     * Extracts the step number from an event modal ID.
     * Returns 1 if no step suffix is present.
     */
    public int getStepFromModalId(String eventModalId) {
        int stepIndex = eventModalId.indexOf("_step_");
        if (stepIndex > 0) {
            try {
                return Integer.parseInt(eventModalId.substring(stepIndex + 6));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * Determines the total number of steps for a modal class.
     * Accounts for nested modal models.
     */
    public int getTotalSteps(Class<?> clazz) {
        List<StepInfo> allSteps = collectAllSteps(clazz, 0);
        return allSteps.isEmpty() ? 1 : allSteps.stream()
                .mapToInt(StepInfo::effectiveStep)
                .max()
                .orElse(1);
    }

    /**
     * Checks if a modal class has multiple steps.
     */
    public boolean isMultiStep(Class<?> clazz) {
        return getTotalSteps(clazz) > 1;
    }

    /**
     * Gets the number of fields for a specific step.
     */
    public int getFieldCountForStep(Class<?> clazz, int step) {
        return collectComponentsForStep(clazz, step, "", 0).size();
    }

    /**
     * Resolves the next step to show after the current step, evaluating {@link ConditionalStep}
     * annotations against session data. Skips steps whose conditions are not met.
     *
     * @param session     the current session with collected values
     * @param currentStep the step that was just completed
     * @param totalSteps  the total number of defined steps
     * @return the next step to show, or a value {@code > totalSteps} if all remaining steps should be skipped
     */
    public int resolveNextStep(ModalSession session, int currentStep, int totalSteps) {
        for (int candidate = currentStep + 1; candidate <= totalSteps; candidate++) {
            if (isStepConditionMet(session.getModalClass(), candidate, session)) {
                return candidate;
            }
        }
        return totalSteps + 1;
    }

    /**
     * Checks whether a step's conditions are met based on the current session values.
     * A step with no {@link ConditionalStep} annotations is always shown.
     * A step with conditions is shown only if ALL conditions are satisfied.
     */
    public boolean isStepConditionMet(Class<?> clazz, int step, ModalSession session) {
        List<ConditionalStep> conditions = collectConditionsForStep(clazz, step, "", 0);
        if (conditions.isEmpty()) {
            return true;
        }
        return conditions.stream().allMatch(cond -> evaluateCondition(cond, session));
    }

    /**
     * Checks if a modal class has any conditional steps.
     */
    public boolean hasConditionalSteps(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ConditionalStep.class)) {
                return true;
            }
            NestedModal nested = field.getAnnotation(NestedModal.class);
            if (nested != null && hasConditionalSteps(field.getType())) {
                return true;
            }
        }
        return false;
    }

    private List<ConditionalStep> collectConditionsForStep(Class<?> clazz, int targetStep, String prefix, int stepOffset) {
        List<ConditionalStep> conditions = new ArrayList<>();
        int currentOffset = stepOffset;

        List<Field> fields = getSortedFields(clazz);

        for (Field field : fields) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                String nestedPrefix = prefix.isEmpty() ? nested.prefix() : prefix + "_" + nested.prefix();
                int nestedMaxStep = getMaxStepForClass(field.getType());
                conditions.addAll(collectConditionsForStep(field.getType(), targetStep, nestedPrefix, currentOffset));
                currentOffset += nestedMaxStep;
            } else {
                int fieldStep = getFieldStep(field);
                if (fieldStep > 0 && (fieldStep + stepOffset) == targetStep) {
                    ConditionalStep condition = field.getAnnotation(ConditionalStep.class);
                    if (condition != null) {
                        conditions.add(condition);
                    }
                }
            }
        }

        return conditions;
    }

    private boolean evaluateCondition(ConditionalStep condition, ModalSession session) {
        Object rawValue = session.getFieldValue(condition.dependsOn());
        String actualValue = rawValue != null ? rawValue.toString() : null;

        boolean matches = false;
        if (actualValue != null) {
            for (String expected : condition.havingValue()) {
                if (expected.equals(actualValue)) {
                    matches = true;
                    break;
                }
            }
        }

        return condition.negate() != matches;
    }

    private Modal getModalAnnotation(Class<?> clazz) {
        Modal annotation = clazz.getAnnotation(Modal.class);
        if (annotation == null) {
            throw new ModalMappingException("Class " + clazz.getName() + " must be annotated with @Modal");
        }
        return annotation;
    }

    private List<StepInfo> collectAllSteps(Class<?> clazz, int stepOffset) {
        List<StepInfo> steps = new ArrayList<>();
        int currentOffset = stepOffset;

        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        fields.sort((f1, f2) -> {
            int order1 = getNestedOrder(f1);
            int order2 = getNestedOrder(f2);
            return Integer.compare(order1, order2);
        });

        for (Field field : fields) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                List<StepInfo> nestedSteps = collectAllSteps(field.getType(), currentOffset);
                steps.addAll(nestedSteps);

                int maxNestedStep = nestedSteps.stream()
                        .mapToInt(StepInfo::effectiveStep)
                        .max()
                        .orElse(currentOffset);
                currentOffset = maxNestedStep;
            } else {
                int fieldStep = getFieldStep(field);
                if (fieldStep > 0) {
                    steps.add(new StepInfo(field, fieldStep + stepOffset, "", null));
                }
            }
        }

        return steps;
    }

    private int getNestedOrder(Field field) {
        NestedModal nested = field.getAnnotation(NestedModal.class);
        if (nested != null) {
            return nested.order();
        }
        int step = getFieldStep(field);
        return step > 0 ? step * 1000 : Integer.MAX_VALUE;
    }

    private int getFieldStep(Field field) {
        TextInput textInput = field.getAnnotation(TextInput.class);
        if (textInput != null) {
            return textInput.step();
        }

        Attachment attachment = field.getAnnotation(Attachment.class);
        if (attachment != null) {
            return attachment.step();
        }

        SelectMenu selectMenu = field.getAnnotation(SelectMenu.class);
        if (selectMenu != null) {
            return selectMenu.step();
        }

        return 0;
    }

    private String getTitleSuffixForStep(Class<?> clazz, int targetStep) {
        return getTitleSuffixRecursive(clazz, targetStep, 0, "");
    }

    private String getTitleSuffixRecursive(Class<?> clazz, int targetStep, int stepOffset, String currentSuffix) {
        int currentOffset = stepOffset;

        List<Field> fields = getSortedFields(clazz);

        for (Field field : fields) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                int nestedMaxStep = getMaxStepForClass(field.getType());
                int nestedStartStep = currentOffset + 1;
                int nestedEndStep = currentOffset + nestedMaxStep;

                if (targetStep >= nestedStartStep && targetStep <= nestedEndStep) {
                    String newSuffix = currentSuffix + nested.titleSuffix();
                    return getTitleSuffixRecursive(field.getType(), targetStep, currentOffset, newSuffix);
                }

                currentOffset = nestedEndStep;
            } else {
                int fieldStep = getFieldStep(field);
                if (fieldStep > 0) {
                    int effectiveStep = fieldStep + stepOffset;
                    if (effectiveStep == targetStep) {
                        return currentSuffix;
                    }
                }
            }
        }

        return currentSuffix;
    }

    private int getMaxStepForClass(Class<?> clazz) {
        int maxStep = 0;
        for (Field field : clazz.getDeclaredFields()) {
            NestedModal nested = field.getAnnotation(NestedModal.class);
            if (nested != null) {
                maxStep += getMaxStepForClass(field.getType());
            } else {
                int step = getFieldStep(field);
                maxStep = Math.max(maxStep, step);
            }
        }
        return maxStep == 0 ? 1 : maxStep;
    }

    private List<Field> getSortedFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        fields.sort((f1, f2) -> {
            int order1 = getNestedOrder(f1);
            int order2 = getNestedOrder(f2);
            return Integer.compare(order1, order2);
        });
        return fields;
    }

    private List<FieldComponent> collectComponentsForStep(Class<?> clazz, int targetStep, String prefix, int stepOffset) {
        List<FieldComponent> components = new ArrayList<>();
        int currentOffset = stepOffset;

        List<Field> fields = getSortedFields(clazz);

        for (Field field : fields) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                String nestedPrefix = prefix.isEmpty() ? nested.prefix() : prefix + "_" + nested.prefix();
                int nestedMaxStep = getMaxStepForClass(field.getType());

                components.addAll(collectComponentsForStep(
                        field.getType(),
                        targetStep,
                        nestedPrefix,
                        currentOffset
                ));

                currentOffset += nestedMaxStep;
            } else {
                TextInput textInput = field.getAnnotation(TextInput.class);
                Attachment attachment = field.getAnnotation(Attachment.class);
                SelectMenu selectMenu = field.getAnnotation(SelectMenu.class);

                if (textInput != null) {
                    int effectiveStep = textInput.step() + stepOffset;
                    if (effectiveStep == targetStep) {
                        components.add(createTextInputComponent(field, textInput, prefix));
                    }
                } else if (attachment != null) {
                    int effectiveStep = attachment.step() + stepOffset;
                    if (effectiveStep == targetStep) {
                        components.add(createAttachmentComponent(field, attachment, prefix));
                    }
                } else if (selectMenu != null) {
                    int effectiveStep = selectMenu.step() + stepOffset;
                    if (effectiveStep == targetStep) {
                        components.add(createSelectMenuComponent(field, selectMenu, prefix));
                    }
                }
            }
        }

        return components;
    }

    private FieldComponent createTextInputComponent(Field field, TextInput annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;

        var inputBuilder = net.dv8tion.jda.api.components.textinput.TextInput
                .create(id, annotation.style())
                .setRequired(annotation.required());

        if (!annotation.placeholder().isEmpty()) {
            inputBuilder.setPlaceholder(annotation.placeholder());
        }

        if (annotation.minLength() > 0) {
            inputBuilder.setMinLength(annotation.minLength());
        }

        if (annotation.maxLength() > 0) {
            inputBuilder.setMaxLength(annotation.maxLength());
        }

        if (!annotation.defaultValue().isEmpty()) {
            inputBuilder.setValue(annotation.defaultValue());
        }

        return new FieldComponent(
                annotation.order(),
                Label.of(annotation.label(), inputBuilder.build())
        );
    }

    private FieldComponent createAttachmentComponent(Field field, Attachment annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;

        var attachmentUpload = AttachmentUpload.create(id)
                .setRequired(annotation.required())
                .build();

        return new FieldComponent(
                annotation.order(),
                Label.of(annotation.label(), attachmentUpload)
        );
    }

    private FieldComponent createSelectMenuComponent(Field field, SelectMenu annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;

        net.dv8tion.jda.api.components.selections.SelectMenu selectMenu;

        if (annotation.type() == SelectMenuType.STRING) {
            var builder = StringSelectMenu.create(id)
                    .setRequiredRange(annotation.minValues(), annotation.maxValues());

            if (!annotation.placeholder().isEmpty()) {
                builder.setPlaceholder(annotation.placeholder());
            }

            for (SelectOption option : annotation.options()) {
                var optionBuilder = net.dv8tion.jda.api.components.selections.SelectOption
                        .of(option.label(), option.value());

                if (!option.description().isEmpty()) {
                    optionBuilder = optionBuilder.withDescription(option.description());
                }

                if (!option.emoji().isEmpty()) {
                    optionBuilder = optionBuilder.withEmoji(Emoji.fromFormatted(option.emoji()));
                }

                if (option.isDefault()) {
                    optionBuilder = optionBuilder.withDefault(true);
                }

                builder.addOptions(optionBuilder);
            }

            selectMenu = builder.build();
        } else {
            EntitySelectMenu.SelectTarget target = switch (annotation.type()) {
                case USER -> EntitySelectMenu.SelectTarget.USER;
                case ROLE -> EntitySelectMenu.SelectTarget.ROLE;
                case CHANNEL -> EntitySelectMenu.SelectTarget.CHANNEL;
                case MENTIONABLE -> EntitySelectMenu.SelectTarget.USER;
                default -> throw new ModalMappingException("Unknown SelectMenuType: " + annotation.type());
            };

            var builder = EntitySelectMenu.create(id, target);

            if (annotation.type() == SelectMenuType.MENTIONABLE) {
                builder = EntitySelectMenu.create(id, EntitySelectMenu.SelectTarget.USER, EntitySelectMenu.SelectTarget.ROLE);
            }

            builder.setRequiredRange(annotation.minValues(), annotation.maxValues());

            if (!annotation.placeholder().isEmpty()) {
                builder.setPlaceholder(annotation.placeholder());
            }

            if (annotation.type() == SelectMenuType.CHANNEL && annotation.channelTypes().length > 0) {
                builder.setChannelTypes(annotation.channelTypes());
            }

            selectMenu = builder.build();
        }

        return new FieldComponent(
                annotation.order(),
                Label.of(annotation.label(), selectMenu)
        );
    }

    private void mapFieldsFromEvent(ModalInteractionEvent event, Object instance, Class<?> clazz, String prefix) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                NestedModal nested = field.getAnnotation(NestedModal.class);

                if (nested != null) {
                    String nestedPrefix = prefix.isEmpty() ? nested.prefix() : prefix + "_" + nested.prefix();
                    Object nestedInstance = field.getType().getDeclaredConstructor().newInstance();
                    mapFieldsFromEvent(event, nestedInstance, field.getType(), nestedPrefix);
                    field.setAccessible(true);
                    field.set(instance, nestedInstance);
                } else {
                    Object value = extractFieldValue(event, field, prefix);
                    if (value != null) {
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new ModalMappingException("Failed to map fields from event", e);
        }
    }

    private void mapStepToSessionRecursive(ModalInteractionEvent event, ModalSession session,
                                            Class<?> clazz, int targetStep, String prefix, int stepOffset) {
        int currentOffset = stepOffset;
        List<Field> fields = getSortedFields(clazz);

        for (Field field : fields) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                String nestedPrefix = prefix.isEmpty() ? nested.prefix() : prefix + "_" + nested.prefix();
                int nestedMaxStep = getMaxStepForClass(field.getType());

                mapStepToSessionRecursive(event, session, field.getType(), targetStep, nestedPrefix, currentOffset);
                currentOffset += nestedMaxStep;
            } else {
                int fieldStep = getFieldStep(field);
                if (fieldStep > 0 && (fieldStep + stepOffset) == targetStep) {
                    Object value = extractFieldValue(event, field, prefix);
                    if (value != null) {
                        String key = prefix.isEmpty() ? field.getName() : prefix + "_" + field.getName();
                        session.setFieldValue(key, value);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T mapFromSessionRecursive(ModalSession session, Class<T> clazz, String prefix)
            throws ReflectiveOperationException {

        T instance = clazz.getDeclaredConstructor().newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            NestedModal nested = field.getAnnotation(NestedModal.class);

            if (nested != null) {
                String nestedPrefix = prefix.isEmpty() ? nested.prefix() : prefix + "_" + nested.prefix();
                Object nestedInstance = mapFromSessionRecursive(session, field.getType(), nestedPrefix);
                field.setAccessible(true);
                field.set(instance, nestedInstance);
            } else if (hasInputAnnotation(field)) {
                String key = prefix.isEmpty() ? field.getName() : prefix + "_" + field.getName();
                Object value = session.getFieldValue(key);
                if (value != null) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }
        }

        return instance;
    }

    private boolean hasInputAnnotation(Field field) {
        return field.isAnnotationPresent(TextInput.class)
                || field.isAnnotationPresent(Attachment.class)
                || field.isAnnotationPresent(SelectMenu.class);
    }

    private Object extractFieldValue(ModalInteractionEvent event, Field field, String prefix) {
        TextInput textInput = field.getAnnotation(TextInput.class);
        if (textInput != null) {
            return extractTextInputValue(event, field, textInput, prefix);
        }

        Attachment attachment = field.getAnnotation(Attachment.class);
        if (attachment != null) {
            return extractAttachmentValue(event, field, attachment, prefix);
        }

        SelectMenu selectMenu = field.getAnnotation(SelectMenu.class);
        if (selectMenu != null) {
            return extractSelectMenuValue(event, field, selectMenu, prefix);
        }

        return null;
    }

    private Object extractTextInputValue(ModalInteractionEvent event, Field field, TextInput annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;
        ModalMapping mapping = event.getValue(id);

        if (mapping == null) {
            return null;
        }

        String value = mapping.getAsString();

        Class<?> type = field.getType();
        if (type == String.class) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return value.isEmpty() ? null : Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            return value.isEmpty() ? null : Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            return value.isEmpty() ? null : Double.parseDouble(value);
        }

        return value;
    }

    private Object extractAttachmentValue(ModalInteractionEvent event, Field field, Attachment annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;
        ModalMapping mapping = event.getValue(id);

        if (mapping == null) {
            return null;
        }

        List<Message.Attachment> attachments = mapping.getAsAttachmentList();

        if (field.getType() == Message.Attachment.class) {
            return attachments.isEmpty() ? null : attachments.getFirst();
        } else if (List.class.isAssignableFrom(field.getType())) {
            return attachments;
        }

        return null;
    }

    private Object extractSelectMenuValue(ModalInteractionEvent event, Field field, SelectMenu annotation, String prefix) {
        String baseId = annotation.id().isEmpty() ? field.getName() : annotation.id();
        String id = prefix.isEmpty() ? baseId : prefix + "_" + baseId;
        ModalMapping mapping = event.getValue(id);

        if (mapping == null) {
            return null;
        }

        Class<?> type = field.getType();
        boolean isList = List.class.isAssignableFrom(type);

        return switch (annotation.type()) {
            case STRING -> {
                List<String> values = mapping.getAsStringList();
                yield isList ? values : (values.isEmpty() ? null : values.getFirst());
            }
            case USER -> {
                List<User> users = mapping.getAsMentions().getUsers();
                if (isList) {
                    yield users;
                } else if (type == User.class) {
                    yield users.isEmpty() ? null : users.getFirst();
                } else if (type == Member.class || Member.class.isAssignableFrom(type)) {
                    List<Member> members = mapping.getAsMentions().getMembers();
                    yield members.isEmpty() ? null : members.getFirst();
                }
                yield users.isEmpty() ? null : users.getFirst();
            }
            case ROLE -> {
                List<Role> roles = mapping.getAsMentions().getRoles();
                yield isList ? roles : (roles.isEmpty() ? null : roles.getFirst());
            }
            case CHANNEL -> {
                List<GuildChannel> channels = mapping.getAsMentions().getChannels();
                yield isList ? channels : (channels.isEmpty() ? null : channels.getFirst());
            }
            case MENTIONABLE -> {
                var mentions = mapping.getAsMentions();
                List<IMentionable> mentionables = new ArrayList<>();
                mentionables.addAll(mentions.getUsers());
                mentionables.addAll(mentions.getRoles());
                yield isList ? mentionables : (mentionables.isEmpty() ? null : mentionables.getFirst());
            }
        };
    }

    private record FieldComponent(int order, Label labeled) {}

    private record StepInfo(Field field, int effectiveStep, String prefix, String titleSuffix) {}
}
