package io.quarkus.test.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.awaitility.core.ConditionTimeoutException;

import io.quarkus.test.bootstrap.Service;

public class LogsVerifier {

    private final Service service;
    private String customMessage;

    public LogsVerifier(Service service) {
        this.service = service;
    }

    public LogsVerifier withMessage(String message) {
        this.customMessage = message;
        return this;
    }

    public QuarkusLogsVerifier forQuarkus() {
        return new QuarkusLogsVerifier(service);
    }

    /**
     * Asserts log contains any of {@code expectedLogs}.
     */
    public void assertContains(String... expectedLogs) {
        Predicate<String> containsExpectedLog = createExpectedLogPredicate(expectedLogs);
        try {
            AwaitilityUtils.untilAsserted(() -> {
                List<String> actualLogs = service.getLogs();
                if (!actualLogs.stream().anyMatch(containsExpectedLog)) {
                    String message = customMessage;
                    if (message == null) {
                        message = "Log does not contain any of '" + Arrays.toString(expectedLogs) + "'. Full logs: "
                                + actualLogs;
                    }
                    throw new AssertionError(message);
                }
            });
        } catch (ConditionTimeoutException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionError) {
                throw (AssertionError) cause;
            }
            throw e;
        }
    }

    private Predicate<String> createExpectedLogPredicate(String[] expectedLogs) {
        if (expectedLogs.length == 1) {
            return log -> log.contains(expectedLogs[0]);
        }
        return log -> Arrays.stream(expectedLogs).anyMatch(log::contains);
    }

    public void assertDoesNotContain(String unexpectedLog) {
        List<String> actualLogs = service.getLogs();
        if (actualLogs.stream().anyMatch(line -> line.contains(unexpectedLog))) {
            String message = customMessage;
            if (message == null) {
                message = "Log does contain " + unexpectedLog + ". Full logs: " + actualLogs;
            }
            throw new AssertionError(message);
        }
    }
}
