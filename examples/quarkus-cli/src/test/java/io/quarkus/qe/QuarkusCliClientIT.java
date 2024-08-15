package io.quarkus.qe;

import static io.quarkus.test.bootstrap.QuarkusCliClient.CreateApplicationRequest.defaults;
import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliDefaultService;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.quarkus.CliDevModeVersionLessQuarkusApplicationManagedResource;

@Tag("quarkus-cli")
@QuarkusScenario
public class QuarkusCliClientIT {

    static final String RESTEASY_REACTIVE_EXTENSION = "quarkus-resteasy-reactive";
    static final String SMALLRYE_HEALTH_EXTENSION = "quarkus-smallrye-health";
    static final int CMD_DELAY_SEC = 3;

    @Inject
    static QuarkusCliClient cliClient;

    @Test
    public void shouldCreateApplicationOnJvm() {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app",
                defaults().withStream("3.8"));

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());

        // Start using DEV mode
        app.start();
        app.given().get().then().statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void shouldCreateExtension() {
        // Create extension
        QuarkusCliDefaultService app = cliClient.createExtension("extension-abc",
                QuarkusCliClient.CreateExtensionRequest.defaults());

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The extension build failed. Output: " + result.getOutput());
    }

    @Test
    public void shouldCreateApplicationUsingArtifactId() {
        QuarkusCliRestService app = cliClient.createApplication("com.mycompany:my-app",
                defaults().withStream("3.8"));
        assertEquals("my-app", app.getServiceFolder().getFileName().toString(), "The application directory differs.");

        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());
    }

    @Test
    public void shouldAddAndRemoveExtensions() throws InterruptedException {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app",
                defaults().withStream("3.8"));

        // By default, it installs only "quarkus-resteasy-reactive"
        assertInstalledExtensions(app, RESTEASY_REACTIVE_EXTENSION);

        // Let's install Quarkus SmallRye Health
        QuarkusCliClient.Result result = app.installExtension(SMALLRYE_HEALTH_EXTENSION);
        assertTrue(result.isSuccessful(), SMALLRYE_HEALTH_EXTENSION + " was not installed. Output: " + result.getOutput());

        // Verify both extensions now
        assertInstalledExtensions(app, RESTEASY_REACTIVE_EXTENSION, SMALLRYE_HEALTH_EXTENSION);

        // The health endpoint should be now available
        app.start();
        untilAsserted(() -> app.given().get("/q/health").then().statusCode(HttpStatus.SC_OK));
        app.stop();

        // Let's now remove the SmallRye Health extension
        result = app.removeExtension(SMALLRYE_HEALTH_EXTENSION);
        assertTrue(result.isSuccessful(), SMALLRYE_HEALTH_EXTENSION + " was not uninstalled. Output: " + result.getOutput());

        // The health endpoint should be now gone
        startAfter(app, Duration.ofSeconds(CMD_DELAY_SEC));
        untilAsserted(() -> app.given().get("/q/health").then().statusCode(HttpStatus.SC_NOT_FOUND));
    }

    private void assertInstalledExtensions(QuarkusCliRestService app, String... expectedExtensions) {
        List<String> extensions = app.getInstalledExtensions();
        Stream.of(expectedExtensions).forEach(expectedExtension -> assertTrue(extensions.contains(expectedExtension),
                expectedExtension + " not found in " + extensions));
    }

    // https://github.com/quarkusio/quarkus/issues/21070
    private void startAfter(QuarkusCliRestService app, Duration duration) throws InterruptedException {
        TimeUnit.SECONDS.sleep(duration.getSeconds());
        app.start();
    }

    @Test
    public void shouldRunApplicationWithoutOverwritingVersion() {
        QuarkusCliRestService app = cliClient.createApplication("versionFull:app", defaults()
                .withStream("3.8")
                .withPlatformBom(null)
                .withManagedResourceCreator((serviceContext,
                        quarkusCliClient) -> managedResBuilder -> new CliDevModeVersionLessQuarkusApplicationManagedResource(
                                serviceContext, quarkusCliClient)));

        app.start();
        String response = app.given().get().getBody().asString();
        // check that app was indeed running with quarkus 3.8 (it was not overwritten)
        // version is printed on welcome screen
        assertTrue(response.contains("3.8"), "Quarkus is not running on 3.8");
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/42567")
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
    public void testCreateApplicationFromExistingSources() {
        Path srcPath = Paths.get("src/test/resources/existingSourcesApp");
        QuarkusCliRestService app = cliClient.createApplicationFromExistingSources("app", null, srcPath);

        app.start();
        Awaitility.await().timeout(15, TimeUnit.SECONDS)
                .untilAsserted(() -> app.given().get("/hello").then().statusCode(HttpStatus.SC_OK));
    }
}
