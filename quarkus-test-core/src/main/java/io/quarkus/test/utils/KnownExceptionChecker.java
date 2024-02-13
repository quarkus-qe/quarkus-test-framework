package io.quarkus.test.utils;

import java.util.List;

public final class KnownExceptionChecker {
    /**
     * List of known exception messages. If you want to mask any exception, add its message here
     */
    private static final List<String> KNOWN_EXCEPTION_MESSAGES = List.of("Broken pipe");

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
        if (KNOWN_EXCEPTION_MESSAGES.stream().anyMatch(throwable.getMessage()::contains)) {
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
    }
}
