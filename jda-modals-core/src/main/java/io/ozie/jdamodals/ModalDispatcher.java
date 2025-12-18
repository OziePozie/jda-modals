package io.ozie.jdamodals;

import io.ozie.jdamodals.i18n.DefaultModalMessages;
import io.ozie.jdamodals.i18n.ModalMessages;
import io.ozie.jdamodals.session.ModalSession;
import io.ozie.jdamodals.session.ModalSessionStorage;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import io.ozie.jdamodals.logging.LoggerWrapper;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Automatically dispatches modal interaction events to the appropriate handlers.
 * Supports both single-step and multi-step wizard modals.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ModalMapper mapper = new ModalMapper();
 * ModalSessionStorage storage = new ModalSessionStorage();
 * ModalDispatcher dispatcher = new ModalDispatcher(handlers, mapper, storage);
 *
 * // Register with JDA
 * jda.addEventListener(dispatcher);
 * }</pre>
 */
public class ModalDispatcher {

    private static final LoggerWrapper log = LoggerWrapper.getLogger(ModalDispatcher.class);
    private static final String CONTINUE_BUTTON_PREFIX = "modal_continue:";

    private final Map<String, ModalHandler<?>> handlers = new HashMap<>();
    private final ModalMapper mapper;
    private final ModalSessionStorage sessionStorage;
    private final ModalMessages messages;

    /**
     * Creates a dispatcher with default messages.
     */
    public ModalDispatcher(List<ModalHandler<?>> handlers, ModalMapper mapper, ModalSessionStorage sessionStorage) {
        this(handlers, mapper, sessionStorage, new DefaultModalMessages());
    }

    /**
     * Creates a dispatcher with custom messages.
     */
    public ModalDispatcher(List<ModalHandler<?>> handlers, ModalMapper mapper,
                           ModalSessionStorage sessionStorage, ModalMessages messages) {
        this.mapper = mapper;
        this.sessionStorage = sessionStorage;
        this.messages = messages;
        registerHandlers(handlers);
    }

    private void registerHandlers(List<ModalHandler<?>> handlers) {
        for (ModalHandler<?> handler : handlers) {
            String modalId = mapper.getModalId(handler.getModalClass());
            this.handlers.put(modalId, handler);

            int totalSteps = mapper.getTotalSteps(handler.getModalClass());
            if (totalSteps > 1) {
                log.info("Registered multi-step modal handler for '{}': {} ({} steps)",
                        modalId, handler.getClass().getSimpleName(), totalSteps);
            } else {
                log.info("Registered modal handler for '{}': {}",
                        modalId, handler.getClass().getSimpleName());
            }
        }
    }

    @SubscribeEvent
    public void onModalInteraction(ModalInteractionEvent event) {
        String eventModalId = event.getModalId();
        String baseModalId = mapper.getBaseModalId(eventModalId);
        int currentStep = mapper.getStepFromModalId(eventModalId);

        ModalHandler<?> handler = handlers.get(baseModalId);
        if (handler == null) {
            return;
        }

        int totalSteps = mapper.getTotalSteps(handler.getModalClass());

        if (totalSteps > 1) {
            handleMultiStepModal(event, handler, baseModalId, currentStep, totalSteps);
        } else {
            handleSingleStepModal(event, handler);
        }
    }

    @SubscribeEvent
    public void onContinueButton(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (!buttonId.startsWith(CONTINUE_BUTTON_PREFIX)) {
            return;
        }

        String[] parts = buttonId.substring(CONTINUE_BUTTON_PREFIX.length()).split(":");
        if (parts.length != 2) {
            return;
        }

        String baseModalId = parts[0];
        int nextStep;
        try {
            nextStep = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        ModalHandler<?> handler = handlers.get(baseModalId);
        if (handler == null) {
            return;
        }

        String userId = event.getUser().getId();

        if (!sessionStorage.hasSession(userId, baseModalId)) {
            Locale locale = toLocale(event.getUserLocale());
            event.reply(messages.sessionExpired(locale))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        var nextModal = mapper.buildForStep(handler.getModalClass(), nextStep);
        event.replyModal(nextModal).queue();

        log.debug("User {} continuing to step {} of modal '{}'", userId, nextStep, baseModalId);
    }

    private <T extends ModalModel> void handleSingleStepModal(ModalInteractionEvent event, ModalHandler<T> handler) {
        try {
            T modal = mapper.map(event, handler.getModalClass());
            handler.handle(event, modal);
        } catch (Exception e) {
            log.error("Error handling modal '{}': {}", event.getModalId(), e.getMessage(), e);
            replyWithError(event);
        }
    }

    private <T extends ModalModel> void handleMultiStepModal(
            ModalInteractionEvent event,
            ModalHandler<T> handler,
            String baseModalId,
            int currentStep,
            int totalSteps) {

        String userId = event.getUser().getId();

        try {
            ModalSession session = sessionStorage.getOrCreateSession(
                    userId, baseModalId, handler.getModalClass(), totalSteps);

            mapper.mapStepToSession(event, session, currentStep);

            if (currentStep < totalSteps) {
                int nextStep = currentStep + 1;
                session.nextStep();

                Locale locale = toLocale(event.getUserLocale());
                String buttonId = CONTINUE_BUTTON_PREFIX + baseModalId + ":" + nextStep;
                String buttonLabel = messages.continueButton(locale);
                Button continueButton = Button.primary(buttonId, buttonLabel);

                String stepMessage = messages.stepCompleted(currentStep, totalSteps, locale);
                event.reply(stepMessage)
                        .addComponents(ActionRow.of(continueButton))
                        .setEphemeral(true)
                        .queue();

                log.debug("User {} completed step {}/{} of modal '{}', waiting for continue",
                        userId, currentStep, totalSteps, baseModalId);
            } else {
                T modal = mapper.mapFromSession(session);
                handler.handle(event, modal);

                sessionStorage.removeSession(userId, baseModalId);

                log.debug("User {} completed modal '{}' ({} steps)",
                        userId, baseModalId, totalSteps);
            }
        } catch (Exception e) {
            log.error("Error handling multi-step modal '{}' step {}: {}",
                    baseModalId, currentStep, e.getMessage(), e);
            sessionStorage.removeSession(userId, baseModalId);
            replyWithError(event);
        }
    }

    private void replyWithError(ModalInteractionEvent event) {
        if (!event.isAcknowledged()) {
            Locale locale = toLocale(event.getUserLocale());
            String errorMessage = messages.error(locale);
            event.reply(errorMessage)
                    .setEphemeral(true)
                    .queue();
        }
    }

    private Locale toLocale(DiscordLocale discordLocale) {
        return Locale.forLanguageTag(discordLocale.getLocale());
    }

    /**
     * Returns the number of registered handlers.
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Checks if a handler exists for the given modal ID.
     */
    public boolean hasHandler(String modalId) {
        return handlers.containsKey(modalId);
    }

    /**
     * Returns the modal mapper used by this dispatcher.
     */
    public ModalMapper getMapper() {
        return mapper;
    }

    /**
     * Returns the session storage used by this dispatcher.
     */
    public ModalSessionStorage getSessionStorage() {
        return sessionStorage;
    }
}
