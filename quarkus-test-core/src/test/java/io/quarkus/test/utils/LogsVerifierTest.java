package io.quarkus.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.Service;

public class LogsVerifierTest {

    private Service service;
    private LogsVerifier verifier;

    @BeforeEach
    public void setup() {
        service = mock(Service.class);
        verifier = new LogsVerifier(service);
    }

    @Test
    public void testAssertContainsWithDefaultMessage() {
        when(service.getLogs()).thenReturn(List.of("relevante log line"));

        AssertionError error = assertThrows(AssertionError.class, () -> {
            verifier.assertContains("missing log");
        });

        assertEquals("Log does not contain any of '[missing log]'. Full logs: [relevante log line]",
                error.getMessage());
    }

    @Test
    public void testAssertContainsWithCustomMessage() {
        when(service.getLogs()).thenReturn(List.of("relevante log line"));
        String customMessage = "Custom failure message - issue 123";

        AssertionError error = assertThrows(AssertionError.class, () -> {
            verifier.withFailureMessage(customMessage).assertContains("missing log");
        });

        assertEquals(customMessage, error.getMessage());
    }

    @Test
    public void testAssertDoesNotContainWithDefaultMessage() {
        List<String> logs = List.of("unexpected log line");
        when(service.getLogs()).thenReturn(logs);

        AssertionError error = assertThrows(AssertionError.class, () -> {
            verifier.assertDoesNotContain("unexpected log line");
        });

        assertEquals("Log does contain unexpected log line. Full logs: [unexpected log line]", error.getMessage());
    }

    @Test
    public void testAssertDoesNotContainWithCustomMessage() {
        when(service.getLogs()).thenReturn(List.of("unexpected log line"));
        String customMessage = "Unexpected string found - issue 456";

        AssertionError error = assertThrows(AssertionError.class, () -> {
            verifier.withFailureMessage(customMessage).assertDoesNotContain("unexpected log line");
        });

        assertEquals(customMessage, error.getMessage());
    }
}
