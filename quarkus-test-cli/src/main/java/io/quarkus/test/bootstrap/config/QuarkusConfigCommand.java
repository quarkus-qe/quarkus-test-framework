package io.quarkus.test.bootstrap.config;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;

public class QuarkusConfigCommand {

    private static final Path CONFIG_COMMAND_TEST_BASE_DIR = Path.of("target").resolve("quarkus-config-command-tests");
    final QuarkusCliRestService app;
    private final QuarkusCliClient cliClient;

    public QuarkusConfigCommand(QuarkusCliClient cliClient) {
        this.cliClient = cliClient;
        var testDirectory = CONFIG_COMMAND_TEST_BASE_DIR.resolve(UUID.randomUUID().toString()).toFile();
        testDirectory.mkdirs();
        this.app = this.cliClient.createApplicationAt("config-command-test", testDirectory.getAbsolutePath());
    }

    public QuarkusConfigCommand withSmallRyeConfigCryptoDep() {
        return addDependency("io.smallrye.config", "smallrye-config-crypto");
    }

    public QuarkusConfigCommand addDependency(String groupId, String artifactId) {
        var newDependency = """
                <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                </dependency>
                """.formatted(groupId, artifactId);
        var pom = this.app.getFileFromApplication("pom.xml");
        var updatedPom = FileUtils.loadFile(pom).transform(str -> {
            if (str.isEmpty()) {
                throw new IllegalArgumentException("The 'addDependency' method called before POM file was available");
            }
            var lastDependenciesClosingTag = str.lastIndexOf("</dependencies>");
            var result = str.substring(0, lastDependenciesClosingTag);
            result += newDependency;
            result += str.substring(lastDependenciesClosingTag);
            return result;
        });
        FileUtils.deleteFileContent(pom);
        FileUtils.copyContentTo(updatedPom, pom.toPath());
        return this;
    }

    public QuarkusConfigCommand addToApplicationPropertiesFile(String... properties) {
        if (properties.length % 2 != 0) {
            throw new IllegalArgumentException("The number of properties must be even.");
        }
        var propsMap = new HashMap<String, String>();
        String key = null;
        for (String p : properties) {
            if (key == null) {
                key = p;
            } else {
                propsMap.put(key, p);
                key = null;
            }
        }
        return addToApplicationPropertiesFile(propsMap);
    }

    public QuarkusConfigCommand addToApplicationPropertiesFile(Map<String, String> properties) {
        properties.forEach((propertyName, propertyValue) -> createProperty()
                .name(propertyName)
                .value(propertyValue)
                .executeCommand()
                .assertApplicationPropertiesContains(propertyName, propertyValue));
        return this;
    }

    public QuarkusSetConfigCommandBuilder createProperty() {
        return new QuarkusSetConfigCommandBuilder(false, this);
    }

    public QuarkusSetConfigCommandBuilder updateProperty() {
        return new QuarkusSetConfigCommandBuilder(true, this);
    }

    public QuarkusSetConfigCommandBuilder setProperty() {
        return new QuarkusSetConfigCommandBuilder(false, this);
    }

    public QuarkusRemoveConfigCommandBuilder removeProperty() {
        return new QuarkusRemoveConfigCommandBuilder(this);
    }

    public QuarkusEncryptConfigCommandBuilder encryptBuilder() {
        return new QuarkusEncryptConfigCommandBuilder(this);
    }

    public QuarkusConfigCommandResult buildAppAndExpectSuccess(Class<?>... unitTests) {
        copyUnitTestsToCreatedApp(unitTests);
        return buildAppAndExpectSuccess();
    }

    public QuarkusConfigCommandResult buildAppAndExpectSuccess() {
        var result = app.buildOnJvm();
        assertTrue(result.isSuccessful(),
                "Expected successful JVM build, but build command failed with output: " + result.getOutput());
        return new QuarkusConfigCommandResult(result.getOutput(), getApplicationPropertiesAsStr());
    }

    public QuarkusConfigCommandResult buildAppAndExpectFailure(Class<?>... unitTests) {
        copyUnitTestsToCreatedApp(unitTests);
        return buildAppAndExpectFailure();
    }

    public QuarkusConfigCommandResult buildAppAndExpectFailure() {
        var result = app.buildOnJvm();
        assertFalse(result.isSuccessful(),
                "Expected JVM build failure, but build command succeed with output: " + result.getOutput());
        return new QuarkusConfigCommandResult(result.getOutput(), getApplicationPropertiesAsStr());
    }

    public void removeApplicationProperties() {
        var appProps = getApplicationProperties();
        if (!appProps.delete()) {
            throw new IllegalStateException("Failed to delete application.properties file: " + appProps);
        }
    }

    QuarkusConfigCommandResult runConfigCommand(List<String> subCmdArgs) {
        var allConfigCommandArgs = new ArrayList<>();
        allConfigCommandArgs.add("config");
        allConfigCommandArgs.addAll(subCmdArgs);
        var result = cliClient.run(app.getServiceFolder(), allConfigCommandArgs.toArray(String[]::new));
        if (!result.isSuccessful()) {
            Assertions.fail("Quarkus config command with arguments '%s' failed with output: %s".formatted(allConfigCommandArgs,
                    result.getOutput()));
        }
        return new QuarkusConfigCommandResult(result.getOutput(), getApplicationPropertiesAsStr());
    }

    private void copyUnitTestsToCreatedApp(Class<?>[] unitTests) {
        if (unitTests == null || unitTests.length == 0) {
            return;
        }
        var normalizedUnitTests = Stream.of(unitTests).map(Class::getName).collect(toSet());
        var srcTestJavaPath = Path.of("src").resolve("test").resolve("java");
        try (Stream<Path> stream = Files.walk(srcTestJavaPath)) {
            stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        var normalizedClassName = ClassPathUtils.normalizeClassName(path.toString(), ".java");
                        return normalizedUnitTests.stream().anyMatch(normalizedClassName::endsWith);
                    })
                    .map(Path::toFile)
                    .forEach(unitTestFile -> FileUtils.copyFileTo(unitTestFile,
                            app.getServiceFolder().resolve(srcTestJavaPath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getApplicationProperties() {
        var pathToSrcMainResources = "src" + File.separator + "main" + File.separator + "resources";
        return app.getFileFromApplication(pathToSrcMainResources, "application.properties");
    }

    String getApplicationPropertiesAsStr() {
        return FileUtils.loadFile(getApplicationProperties());
    }

}
