package io.ozie.jdamodals.session;

import io.ozie.jdamodals.ModalModel;
import io.ozie.jdamodals.logging.LoggerWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for modal sessions.
 * Call {@link #cleanupExpiredSessions()} periodically to remove expired sessions,
 * or use the Spring Boot starter which handles this automatically.
 */
public class ModalSessionStorage {

    private static final LoggerWrapper log = LoggerWrapper.getLogger(ModalSessionStorage.class);

    private final Duration sessionTtl;
    private final Map<String, ModalSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a session storage with the default TTL of 15 minutes.
     */
    public ModalSessionStorage() {
        this(Duration.ofMinutes(15));
    }

    /**
     * Creates a session storage with a custom TTL.
     *
     * @param sessionTtl the time-to-live for sessions
     */
    public ModalSessionStorage(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    /**
     * Creates a new session for a user and modal.
     */
    public ModalSession createSession(String userId, String modalId,
                                       Class<? extends ModalModel> modalClass, int totalSteps) {
        String key = ModalSession.generateKey(userId, modalId);
        ModalSession session = new ModalSession(userId, modalClass, totalSteps);
        sessions.put(key, session);
        log.debug("Created modal session for user {} modal {}, {} steps", userId, modalId, totalSteps);
        return session;
    }

    /**
     * Gets an existing session.
     */
    public Optional<ModalSession> getSession(String userId, String modalId) {
        String key = ModalSession.generateKey(userId, modalId);
        return Optional.ofNullable(sessions.get(key));
    }

    /**
     * Gets or creates a session.
     */
    public ModalSession getOrCreateSession(String userId, String modalId,
                                            Class<? extends ModalModel> modalClass, int totalSteps) {
        return getSession(userId, modalId)
                .orElseGet(() -> createSession(userId, modalId, modalClass, totalSteps));
    }

    /**
     * Removes a session (e.g., after completion or cancellation).
     */
    public void removeSession(String userId, String modalId) {
        String key = ModalSession.generateKey(userId, modalId);
        sessions.remove(key);
        log.debug("Removed modal session for user {} modal {}", userId, modalId);
    }

    /**
     * Checks if a session exists.
     */
    public boolean hasSession(String userId, String modalId) {
        String key = ModalSession.generateKey(userId, modalId);
        return sessions.containsKey(key);
    }

    /**
     * Cleans up expired sessions.
     * Call this method periodically (e.g., every 15 minutes) to remove stale sessions.
     *
     * @return the number of sessions removed
     */
    public int cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minus(sessionTtl);
        int removed = 0;

        var iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().getCreatedAt().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} expired modal sessions", removed);
        }

        return removed;
    }

    /**
     * Returns the number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Returns the configured session TTL.
     */
    public Duration getSessionTtl() {
        return sessionTtl;
    }
}
