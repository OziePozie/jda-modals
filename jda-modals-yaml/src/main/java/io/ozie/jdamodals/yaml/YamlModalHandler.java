package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalHandler;
import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.StepResolver;
import io.ozie.jdamodals.annotation.Attachment;
import io.ozie.jdamodals.annotation.SelectMenu;
import io.ozie.jdamodals.annotation.TextInput;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A turn-key {@link ModalHandler} for modal classes produced by {@link YamlModalLoader}.
 * Since the generated class has no compile-time handles (no class, no getters), this
 * handler exposes submitted values as a {@code Map<String, Object>} keyed by field name.
 *
 * <pre>{@code
 * Class<? extends ModalModel> clazz = new YamlModalLoader().load(yaml);
 *
 * ModalHandler<?> handler = new YamlModalHandler(clazz, (event, values) -> {
 *     String name = (String) values.get("fullName");
 *     event.reply("Hi " + name).queue();
 * });
 *
 * dispatcher.register(handler);  // works with ModalDispatcher as-is
 * }</pre>
 *
 * <p>Values carry the types declared in YAML: {@code String} / {@code Integer} / {@code Long} /
 * {@code Double} for text inputs, {@code Message.Attachment} (or {@code List}) for attachments,
 * and the selected entity (or its {@code List}) for select menus. Unfilled optional fields
 * appear in the map as {@code null} values.
 */
public class YamlModalHandler implements ModalHandler<ModalModel> {

    private final Class<? extends ModalModel> modalClass;
    private final BiConsumer<ModalInteractionEvent, Map<String, Object>> consumer;
    private final StepResolver stepResolver;

    public YamlModalHandler(Class<? extends ModalModel> modalClass,
                            BiConsumer<ModalInteractionEvent, Map<String, Object>> consumer) {
        this(modalClass, consumer, null);
    }

    public YamlModalHandler(Class<? extends ModalModel> modalClass,
                            BiConsumer<ModalInteractionEvent, Map<String, Object>> consumer,
                            StepResolver stepResolver) {
        this.modalClass = modalClass;
        this.consumer = consumer;
        this.stepResolver = stepResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<ModalModel> getModalClass() {
        return (Class<ModalModel>) modalClass;
    }

    @Override
    public void handle(ModalInteractionEvent event, ModalModel modal) {
        consumer.accept(event, valuesOf(modal));
    }

    @Override
    public StepResolver getStepResolver() {
        return stepResolver;
    }

    /**
     * Reads all input-annotated fields from the instance into an ordered map.
     * Field order matches declaration order. Absent values are kept as {@code null}.
     */
    public static Map<String, Object> valuesOf(ModalModel instance) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (!hasInputAnnotation(field)) {
                continue;
            }
            field.setAccessible(true);
            try {
                values.put(field.getName(), field.get(instance));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not read field " + field.getName(), e);
            }
        }
        return values;
    }

    private static boolean hasInputAnnotation(Field field) {
        return field.isAnnotationPresent(TextInput.class)
                || field.isAnnotationPresent(Attachment.class)
                || field.isAnnotationPresent(SelectMenu.class);
    }
}
