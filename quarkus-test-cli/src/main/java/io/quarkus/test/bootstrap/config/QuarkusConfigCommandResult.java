package io.quarkus.test.bootstrap.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.common.os.OS;

public class QuarkusConfigCommandResult {

    final String applicationPropertiesAsString;
    final String output;

    QuarkusConfigCommandResult(String output, String applicationPropertiesAsString) {
        this.output = output;
        this.applicationPropertiesAsString = applicationPropertiesAsString;
    }

    public QuarkusConfigCommandResult assertCommandOutputNotContains(String expected) {
        assertFalse(output.contains(expected.trim()), "Expected output '" + output + "' contains '" + expected + "'");
        return this;
    }

    public QuarkusConfigCommandResult assertCommandOutputContains(String expected) {
        if (OS.WINDOWS.isCurrent()) {
            String windowsEscapedExpected = normalizeString(expected);
            String windowsEscapedOutput = normalizeString(output);

            assertTrue(windowsEscapedOutput.contains(windowsEscapedExpected),
                    "Expected output '" + windowsEscapedExpected + "'does not contain '" + windowsEscapedOutput + "'");
        } else {
            assertTrue(output.contains(expected.trim()),
                    "Expected output '" + output + "' does not contain '" + expected + "'");
        }
        return this;
    }

    private String normalizeString(String str) {
        String noAnsi = str.replaceAll("\\x1B\\[[;\\d]*m", "");
        return noAnsi.replaceAll("\"", "").replaceAll("\n", " ").trim();
    }

    public QuarkusConfigCommandResult assertApplicationPropertiesContains(String str) {
        assertTrue(applicationPropertiesAsString.contains(str),
                "Expected value '" + str + "' is missing in application.properties: " + applicationPropertiesAsString);
        return this;
    }

    public QuarkusConfigCommandResult assertApplicationPropertiesContains(String key, String value) {
        return assertApplicationPropertiesContains(key + "=" + value);
    }

    public QuarkusConfigCommandResult assertApplicationPropertiesDoesNotContain(String key, String value) {
        return assertApplicationPropertiesDoesNotContain(key + "=" + value);
    }

    public QuarkusConfigCommandResult assertApplicationPropertiesDoesNotContain(String str) {
        assertFalse(applicationPropertiesAsString.contains(str),
                "Expected value '" + str + "' is present in application.properties: " + applicationPropertiesAsString);
        return this;
    }
}
