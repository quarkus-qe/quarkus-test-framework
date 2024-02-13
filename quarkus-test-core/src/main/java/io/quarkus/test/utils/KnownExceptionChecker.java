package io.quarkus.test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.quarkus.test.logging.Log;

public final class KnownExceptionChecker {

    /**
     * Define all handlers, that can be used to ignore exceptions.
     * If you want to ignore another exception, just add another predicate and enable it via system property.
     */
    private static final Map<String, Predicate<Throwable>> ALL_EXCEPTION_HANDLERS = Map.of("broken-pipe", throwable -> {
        if (throwable.getMessage().contains("Broken pipe")) {
            return true;
        }
        if (throwable.getCause() != null && checkForKnownException(throwable.getCause())) {
            return true;
        }
        for (Throwable t : throwable.getSuppressed()) {
            if (checkForKnownException(t)) {
                return true;
            }
        }
        return false;
    });

    /**
     * List to store enabled handlers, which are going to be effective.
     */
    private static List<Predicate<Throwable>> enabledExceptionsHandlers = new ArrayList<>();

    /*
     * Read system property and assign desired handlers as enabled ones
     */
    static {
        String enableHandlersParam = System.getProperty("ts.global.ignore-known-issue");
        if (enableHandlersParam != null && !enableHandlersParam.isEmpty()) {
            String[] splitParameters = enableHandlersParam.split(",");
            for (String enableHandler : splitParameters) {
                if (ALL_EXCEPTION_HANDLERS.containsKey(enableHandler.trim())) {
                    enabledExceptionsHandlers.add(ALL_EXCEPTION_HANDLERS.get(enableHandler.trim()));
                } else {
                    Log.warn("Trying to enable unknown exception handler: " + enableHandler);
                }
            }
        }
    }

    private KnownExceptionChecker() {
    }

    /**
     * Check if thrown message contains any of the known exception messages.
     * Method is recursive, it will check the exception itself and all causes and suppressed ones as well.
     *
     * @param throwable Exception to check
     * @return true if any known exception message is included, false otherwise
     */
    public static boolean checkForKnownException(Throwable throwable) {
        for (Predicate<Throwable> p : enabledExceptionsHandlers) {
            if (p.test(throwable)) {
                return true;
            }
        }
        return false;
    }
}
