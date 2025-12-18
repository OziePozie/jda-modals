package io.ozie.jdamodals.logging;

/**
 * A wrapper for optional SLF4J logging.
 * If SLF4J is not present on the classpath, logging calls are silently ignored.
 */
public final class LoggerWrapper {

    private static final boolean SLF4J_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("org.slf4j.LoggerFactory");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        SLF4J_AVAILABLE = available;
    }

    private final Object logger;

    private LoggerWrapper(Class<?> clazz) {
        if (SLF4J_AVAILABLE) {
            this.logger = org.slf4j.LoggerFactory.getLogger(clazz);
        } else {
            this.logger = null;
        }
    }

    public static LoggerWrapper getLogger(Class<?> clazz) {
        return new LoggerWrapper(clazz);
    }

    public void info(String message, Object... args) {
        if (SLF4J_AVAILABLE && logger != null) {
            ((org.slf4j.Logger) logger).info(message, args);
        }
    }

    public void debug(String message, Object... args) {
        if (SLF4J_AVAILABLE && logger != null) {
            ((org.slf4j.Logger) logger).debug(message, args);
        }
    }

    public void error(String message, Object... args) {
        if (SLF4J_AVAILABLE && logger != null) {
            ((org.slf4j.Logger) logger).error(message, args);
        }
    }
}
