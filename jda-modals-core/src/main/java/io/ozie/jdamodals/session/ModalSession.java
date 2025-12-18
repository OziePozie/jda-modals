package io.ozie.jdamodals.session;

import io.ozie.jdamodals.ModalModel;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores intermediate data for multi-step modal wizards.
 * Each session is tied to a user and a specific modal class.
 */
public class ModalSession {

    private final String userId;
    private final Class<? extends ModalModel> modalClass;
    private final Map<String, Object> fieldValues;
    private final int totalSteps;
    private int currentStep;
    private final Instant createdAt;

    public ModalSession(String userId, Class<? extends ModalModel> modalClass, int totalSteps) {
        this.userId = userId;
        this.modalClass = modalClass;
        this.totalSteps = totalSteps;
        this.currentStep = 1;
        this.fieldValues = new HashMap<>();
        this.createdAt = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public Class<? extends ModalModel> getModalClass() {
        return modalClass;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void nextStep() {
        if (currentStep < totalSteps) {
            currentStep++;
        }
    }

    public boolean isLastStep() {
        return currentStep >= totalSteps;
    }

    public boolean isCompleted() {
        return currentStep > totalSteps;
    }

    public void setFieldValue(String fieldName, Object value) {
        fieldValues.put(fieldName, value);
    }

    public Object getFieldValue(String fieldName) {
        return fieldValues.get(fieldName);
    }

    public Map<String, Object> getAllFieldValues() {
        return new HashMap<>(fieldValues);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Generates a unique session key for storage.
     */
    public static String generateKey(String userId, String modalId) {
        return userId + ":" + modalId;
    }
}
