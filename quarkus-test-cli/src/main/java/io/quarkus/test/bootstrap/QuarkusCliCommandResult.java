package io.quarkus.test.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.PropertiesUtils;
import io.smallrye.common.os.OS;

public class QuarkusCliCommandResult {

    protected final String applicationPropertiesAsString;
    protected final String output;
    private final AbstractCliCommand cliCommand;

    public QuarkusCliCommandResult(String output, String applicationPropertiesAsString, AbstractCliCommand cliCommand) {
        this.output = output;
        this.applicationPropertiesAsString = applicationPropertiesAsString;
        this.cliCommand = cliCommand;
    }

    public QuarkusCliCommandResult assertCommandOutputNotContains(String expected) {
        assertFalse(output.contains(expected.trim()), "Expected output '" + output + "' contains '" + expected + "'");
        return this;
    }

    public QuarkusCliCommandResult assertCommandOutputContains(String expected) {
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

    public QuarkusCliCommandResult assertApplicationPropertiesContains(String str) {
        assertTrue(applicationPropertiesAsString.contains(str),
                "Expected value '" + str + "' is missing in application.properties: " + applicationPropertiesAsString);
        return this;
    }

    public QuarkusCliCommandResult assertApplicationPropertiesContains(String key, String value) {
        return assertApplicationPropertiesContains(key + "=" + value);
    }

    public QuarkusCliCommandResult assertApplicationPropertiesDoesNotContain(String key, String value) {
        return assertApplicationPropertiesDoesNotContain(key + "=" + value);
    }

    public QuarkusCliCommandResult assertApplicationPropertiesDoesNotContain(String str) {
        assertFalse(applicationPropertiesAsString.contains(str),
                "Expected value '" + str + "' is present in application.properties: " + applicationPropertiesAsString);
        return this;
    }

    public QuarkusCliCommandResult assertFileExistsStr(Function<QuarkusCliCommandResult, String> resultToPath) {
        return assertFileExists(cmd -> Path.of(resultToPath.apply(this)));
    }

    public QuarkusCliCommandResult assertFileExists(String path) {
        return assertFileExists(Path.of(path));
    }

    public QuarkusCliCommandResult assertFileExists(Function<QuarkusCliCommandResult, Path> resultToPath) {
        var path = resultToPath.apply(this);
        assertTrue(Files.exists(path), "File '%s' does not exist".formatted(path));
        return this;
    }

    public QuarkusCliCommandResult assertFileExists(Path path) {
        assertTrue(Files.exists(path));
        return this;
    }

    public QuarkusCliCommandResult assertFileDoesNotExist(Path path) {
        assertFalse(Files.exists(path), "File '%s' does exist".formatted(path));
        return this;
    }

    public String getPropertyValueFromAppProps(String key) {
        var props = PropertiesUtils.toMap(getApplicationPropertiesAsString());
        return props.get(key);
    }

    public String getPropertyValueFromEnvFile(String key) {
        var props = PropertiesUtils.toMap(getLocalEnvFile().toPath());
        return props.get(key);
    }

    public String getOutput() {
        return output;
    }

    public String getApplicationPropertiesAsString() {
        return applicationPropertiesAsString;
    }

    public String getOutputLineRemainder(String searchedTxt) {
        return get1stOutputLineContaining(searchedTxt)
                .map(t -> {
                    var idx = t.indexOf(searchedTxt);
                    if (idx != -1) {
                        return t.substring(idx + searchedTxt.length()).trim();
                    } else {
                        return null;
                    }
                })
                .map(t -> {
                    if (t.endsWith(".")) {
                        // it's just not convenient to verify dot when you are looking for what is between colon and dot
                        return t.substring(0, t.length() - 1);
                    }
                    return t;
                })
                .orElseGet(() -> Assertions.fail("Searched text '" + searchedTxt + "' is missing in output: " + output));
    }

    public Optional<String> get1stOutputLineContaining(String searchedTxt) {
        return output.lines().filter(l -> l.contains(searchedTxt)).findFirst();
    }

    public File getLocalEnvFile() {
        return cliCommand.getApp().getServiceFolder().resolve(".env").toFile();
    }

    public String getLocalEnvFileContent() {
        return FileUtils.loadFile(getLocalEnvFile());
    }

    public QuarkusCliCommandResult addToAppProps(Function<QuarkusCliCommandResult, String> resultToConfigProp) {
        cliCommand.addToApplicationProperties(System.lineSeparator() + resultToConfigProp.apply(this));
        return this;
    }
}
