package io.quarkus.test.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.Service;

public class LogsVerifier {

    private final Service service;

    public LogsVerifier(Service service) {
        this.service = service;
    }

    public QuarkusLogsVerifier forQuarkus() {
        return new QuarkusLogsVerifier(service);
    }

    /**
     * Asserts log contains any of {@code expectedLogs}.
     */
    public void assertContains(String... expectedLogs) {
        Predicate<String> containsExpectedLog = createExpectedLogPredicate(expectedLogs);
        AwaitilityUtils.untilAsserted(() -> {
            List<String> actualLogs = service.getLogs();
            Assertions.assertTrue(actualLogs.stream().anyMatch(containsExpectedLog),
                    "Log does not contain any of '" + Arrays.toString(expectedLogs) + "'. Full logs: " + actualLogs);
        });
    }

    private Predicate<String> createExpectedLogPredicate(String[] expectedLogs) {
        if (expectedLogs.length == 1) {
            return log -> log.contains(expectedLogs[0]);
        }
        return log -> Arrays.stream(expectedLogs).anyMatch(log::contains);
    }

    public void assertDoesNotContain(String unexpectedLog) {
        List<String> actualLogs = service.getLogs();
        Assertions.assertTrue(actualLogs.stream().noneMatch(line -> line.contains(unexpectedLog)),
                "Log does contain " + unexpectedLog + ". Full logs: " + actualLogs);
    }
}
