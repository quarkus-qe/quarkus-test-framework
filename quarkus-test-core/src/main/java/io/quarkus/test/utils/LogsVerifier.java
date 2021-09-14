package io.quarkus.test.utils;

import java.util.List;

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

    public void assertContains(String expectedLog) {
        AwaitilityUtils.untilAsserted(() -> {
            List<String> actualLogs = service.getLogs();
            Assertions.assertTrue(actualLogs.stream().anyMatch(line -> line.contains(expectedLog)),
                    "Log does not contain " + expectedLog + ". Full logs: " + actualLogs);
        });
    }

    public void assertDoesNotContain(String unexpectedLog) {
        List<String> actualLogs = service.getLogs();
        Assertions.assertTrue(actualLogs.stream().noneMatch(line -> line.contains(unexpectedLog)),
                "Log does contain " + unexpectedLog + ". Full logs: " + actualLogs);
    }
}
