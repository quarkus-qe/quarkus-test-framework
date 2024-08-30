package io.quarkus.test.bootstrap;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import io.quarkus.test.utils.ClassPathUtils;
import io.quarkus.test.utils.FileUtils;

public abstract class AbstractCliCommand {

    protected final QuarkusCliClient cliClient;
    protected final QuarkusCliRestService app;

    public AbstractCliCommand(String appName, QuarkusCliClient.CreateApplicationRequest appReq,
            QuarkusCliClient cliClient, File testDirectory) {
        this.cliClient = cliClient;
        testDirectory.mkdirs();
        this.app = this.cliClient.createApplication(appName, appReq, testDirectory.getAbsolutePath());
    }

    public AbstractCliCommand(String appName, String targetSubDir, QuarkusCliClient.CreateApplicationRequest appReq,
            QuarkusCliClient cliClient) {
        this(appName, appReq, cliClient,
                Path.of("target").resolve(targetSubDir).resolve(UUID.randomUUID().toString()).toFile());
    }

    public abstract AbstractCliCommand addToApplicationProperties(String... additions);

    public File getApplicationProperties() {
        var pathToSrcMainResources = "src" + File.separator + "main" + File.separator + "resources";
        return app.getFileFromApplication(pathToSrcMainResources, "application.properties");
    }

    public String getApplicationPropertiesAsString() {
        return FileUtils.loadFile(getApplicationProperties());
    }

    public QuarkusCliCommandResult buildAppAndExpectSuccess(Class<?>... unitTests) {
        copyUnitTestsToCreatedApp(unitTests);
        return buildAppAndExpectSuccess();
    }

    public QuarkusCliCommandResult buildAppAndExpectSuccess() {
        var result = app.buildOnJvm();
        assertTrue(result.isSuccessful(),
                "Expected successful JVM build, but build command failed with output: " + result.getOutput());
        return new QuarkusCliCommandResult(result.getOutput(), getApplicationPropertiesAsString(), this);
    }

    public QuarkusCliCommandResult buildAppAndExpectFailure(Class<?>... unitTests) {
        copyUnitTestsToCreatedApp(unitTests);
        return buildAppAndExpectFailure();
    }

    public QuarkusCliCommandResult buildAppAndExpectFailure() {
        var result = app.buildOnJvm();
        assertFalse(result.isSuccessful(),
                "Expected JVM build failure, but build command succeed with output: " + result.getOutput());
        return new QuarkusCliCommandResult(result.getOutput(), getApplicationPropertiesAsString(), this);
    }

    public void removeApplicationProperties() {
        var appProps = getApplicationProperties();
        if (!appProps.delete()) {
            throw new IllegalStateException("Failed to delete application.properties file: " + appProps);
        }
    }

    protected QuarkusCliCommandResult runCommand(String baseCmd, List<String> subCmdArgs) {
        var allConfigCommandArgs = new ArrayList<>();
        allConfigCommandArgs.add(baseCmd);
        allConfigCommandArgs.addAll(subCmdArgs);
        var result = cliClient.run(app.getServiceFolder(), allConfigCommandArgs.toArray(String[]::new));
        if (!result.isSuccessful()) {
            Assertions.fail("Quarkus %s command with arguments '%s' failed with output: %s".formatted(baseCmd,
                    allConfigCommandArgs, result.getOutput()));
        }
        return new QuarkusCliCommandResult(result.getOutput(), getApplicationPropertiesAsString(), this);
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

    public QuarkusCliRestService getApp() {
        return app;
    }

    public QuarkusCliClient getCliClient() {
        return cliClient;
    }
}
