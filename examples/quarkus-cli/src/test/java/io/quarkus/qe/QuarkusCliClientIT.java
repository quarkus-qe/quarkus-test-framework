package io.quarkus.qe;

import static io.quarkus.test.bootstrap.QuarkusCliClient.CreateApplicationRequest.defaults;
import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliDefaultService;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.bootstrap.QuarkusVersionAwareCliClient;
import io.quarkus.test.bootstrap.config.QuarkusConfigCommand;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.TestQuarkusCli;
import io.quarkus.test.scenarios.annotations.EnabledOnNative;
import io.quarkus.test.services.quarkus.CliDevModeVersionLessQuarkusApplicationManagedResource;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

@Tag("quarkus-cli")
@QuarkusScenario
public class QuarkusCliClientIT {

    static final String REST_SPRING_WEB_EXTENSION = "quarkus-spring-web";
    static final String REST_EXTENSION = "quarkus-rest";
    static final String REST_JACKSON_EXTENSION = "quarkus-rest-jackson";
    static final String SMALLRYE_HEALTH_EXTENSION = "quarkus-smallrye-health";

    @Inject
    static QuarkusCliClient cliClient;

    @Inject
    static QuarkusConfigCommand configCommand;

    @Test
    public void shouldVersionMatchQuarkusVersion() {
        // Using option
        assertEquals(QuarkusProperties.getVersion(), cliClient.run("--version").getOutput());

        // Using shortcut
        assertEquals(QuarkusProperties.getVersion(), cliClient.run("-v").getOutput());
    }

    @TestQuarkusCli
    public void shouldCreateApplicationOnJvm(QuarkusVersionAwareCliClient cliClient) {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());

        // Start using DEV mode
        app.start();
        app.given().get().then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    @EnabledOnNative
    public void shouldBuildApplicationOnNativeUsingDocker() {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // Should build on Native
        QuarkusCliClient.Result result = app.buildOnNative();
        assertTrue(result.isSuccessful(),
                "The application didn't build on Native. Output: " + result.getOutput());
    }

    @TestQuarkusCli
    public void shouldCreateApplicationWithCodeStarter(QuarkusVersionAwareCliClient cliClient) {
        // Create application with Resteasy Jackson
        QuarkusCliRestService app = cliClient.createApplication("app", cliClient.getDefaultCreateApplicationRequest()
                .withExtensions(REST_SPRING_WEB_EXTENSION, REST_JACKSON_EXTENSION));

        // Verify By default, it installs only "quarkus-resteasy"
        assertInstalledExtensions(app, REST_SPRING_WEB_EXTENSION, REST_JACKSON_EXTENSION);

        // Start using DEV mode
        app.start();
        untilAsserted(() -> app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).and().body(is("Hello Spring")));
    }

    @TestQuarkusCli
    public void shouldCreateExtension(QuarkusVersionAwareCliClient cliClient) {
        // Create extension
        QuarkusCliDefaultService app = cliClient.createExtension("extension-abc");

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The extension build failed. Output: " + result.getOutput());
    }

    @TestQuarkusCli
    public void shouldCreateApplicationUsingArtifactId(QuarkusVersionAwareCliClient cliClient) {
        QuarkusCliRestService app = cliClient.createApplication("com.mycompany:my-app");
        assertEquals("my-app", app.getServiceFolder().getFileName().toString(), "The application directory differs.");

        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());
    }

    @Test
    public void shouldRunApplicationWithoutOverwritingVersion() {
        QuarkusCliRestService app = cliClient.createApplication("versionFull:app", defaults()
                .withStream("3.27")
                .withPlatformBom(null)
                .withManagedResourceCreator((serviceContext,
                        quarkusCliClient) -> managedResourceBuilder -> new CliDevModeVersionLessQuarkusApplicationManagedResource(
                                serviceContext, quarkusCliClient)));

        app.start();
        // check that app was indeed running with quarkus 3.27 (it was not overwritten)
        app.logs().assertContains("powered by Quarkus 3.27");
    }

    @TestQuarkusCli
    public void shouldAddAndRemoveExtensions(QuarkusVersionAwareCliClient cliClient) {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // By default, it installs only "quarkus-rest"
        assertInstalledExtensions(app, REST_EXTENSION);

        // Let's install Quarkus SmallRye Health
        QuarkusCliClient.Result result = app.installExtension(SMALLRYE_HEALTH_EXTENSION);
        assertTrue(result.isSuccessful(), SMALLRYE_HEALTH_EXTENSION + " was not installed. Output: " + result.getOutput());

        // Verify both extensions now
        assertInstalledExtensions(app, REST_EXTENSION, SMALLRYE_HEALTH_EXTENSION);

        // The health endpoint should be now available
        app.start();
        untilAsserted(() -> app.given().get("/q/health").then().statusCode(HttpStatus.SC_OK));
        app.stop();

        // Let's now remove the SmallRye Health extension
        result = app.removeExtension(SMALLRYE_HEALTH_EXTENSION);
        assertTrue(result.isSuccessful(), SMALLRYE_HEALTH_EXTENSION + " was not uninstalled. Output: " + result.getOutput());

        // The health endpoint should be now gone
        app.start();
        untilAsserted(() -> app.given().get("/q/health").then().statusCode(HttpStatus.SC_NOT_FOUND));
    }

    @Test
    public void shouldListExtensionsUsingDefaults() {
        var result = cliClient.listExtensions();
        assertTrue(result.getOutput().contains("quarkus-rest-jackson"),
                "Listed extensions should contain quarkus-rest-jackson: " + result.getOutput());
    }

    @Test
    public void shouldUpdateApplication() throws IOException {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app", defaults()
                // force CLI to omit platform BOM
                .withPlatformBom(null)
                .withStream("3.2"));

        // Update application
        QuarkusCliClient.Result result = app
                .update(QuarkusCliClient.UpdateApplicationRequest.defaultUpdate().withStream("3.8"));
        File pom = app.getFileFromApplication("pom.xml");
        assertTrue(FileUtils.readFileToString(pom, Charset.defaultCharset()).contains("<quarkus.platform.version>3.8"),
                "Quarkus was not updated to 3.8 stream: " + result.getOutput());
    }

    @Test
    public void testConfigCommand() {
        var propertyName = "property-name-1";
        configCommand.createProperty()
                .name(propertyName)
                .value("val1")
                .executeCommand()
                .assertApplicationPropertiesContains(propertyName, "val1");
        configCommand.updateProperty()
                .name(propertyName)
                .encrypt()
                .executeCommand()
                .assertApplicationPropertiesContains(propertyName)
                .assertApplicationPropertiesDoesNotContain("val1");
        configCommand.removeProperty()
                .name(propertyName)
                .executeCommand()
                .assertApplicationPropertiesDoesNotContain(propertyName);
    }

    @Test
    public void testCreateApplicationFromExistingSources() {
        Path srcPath = Paths.get("src/test/resources/existingSourcesApp");
        QuarkusCliRestService app = cliClient.createApplicationFromExistingSources("app", null, srcPath);

        app.start();
        Awaitility.await().timeout(15, TimeUnit.SECONDS)
                .untilAsserted(() -> app.given().get("/hello").then().statusCode(HttpStatus.SC_OK));
    }

    private void assertInstalledExtensions(QuarkusCliRestService app, String... expectedExtensions) {
        List<String> extensions = app.getInstalledExtensions();
        Stream.of(expectedExtensions).forEach(expectedExtension -> assertTrue(extensions.contains(expectedExtension),
                expectedExtension + " not found in " + extensions));
    }
}
