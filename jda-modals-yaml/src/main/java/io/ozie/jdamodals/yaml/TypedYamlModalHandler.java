package io.ozie.jdamodals.yaml;

import io.ozie.jdamodals.ModalHandler;
import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.StepResolver;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.util.function.BiConsumer;

/**
 * A {@link ModalHandler} for classes produced by
 * {@link YamlModalLoader#bind(String, Class) YamlModalLoader.bind(...)}.
 * The callback receives a typed view of the submitted data via the user-supplied interface.
 *
 * <pre>{@code
 * public interface RegistrationForm {
 *     String fullName();
 *     Integer age();
 * }
 *
 * Class<? extends ModalModel> clazz = loader.bind(yaml, RegistrationForm.class);
 *
 * ModalHandler<?> handler = new TypedYamlModalHandler<>(
 *         clazz, RegistrationForm.class,
 *         (event, form) -> event.reply("Hi " + form.fullName()).queue());
 * }</pre>
 *
 * @param <V> the user-defined view interface
 */
public class TypedYamlModalHandler<V> implements ModalHandler<ModalModel> {

    private final Class<? extends ModalModel> modalClass;
    private final Class<V> view;
    private final BiConsumer<ModalInteractionEvent, V> consumer;
    private final StepResolver stepResolver;

    public TypedYamlModalHandler(Class<? extends ModalModel> modalClass,
                                 Class<V> view,
                                 BiConsumer<ModalInteractionEvent, V> consumer) {
        this(modalClass, view, consumer, null);
    }

    public TypedYamlModalHandler(Class<? extends ModalModel> modalClass,
                                 Class<V> view,
                                 BiConsumer<ModalInteractionEvent, V> consumer,
                                 StepResolver stepResolver) {
        if (!view.isAssignableFrom(modalClass)) {
            throw new IllegalArgumentException(
                    modalClass.getName() + " does not implement " + view.getName()
                            + " — did you load it with bind()?");
        }
        this.modalClass = modalClass;
        this.view = view;
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
        consumer.accept(event, view.cast(modal));
    }

    @Override
    public StepResolver getStepResolver() {
        return stepResolver;
    }
}
