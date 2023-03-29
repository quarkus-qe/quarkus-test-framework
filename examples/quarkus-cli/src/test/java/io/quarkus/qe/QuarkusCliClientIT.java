package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliDefaultService;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;

@DisabledOnQuarkusSnapshot(reason = "https://github.com/quarkusio/quarkus/issues/32219")
@Tag("quarkus-cli")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus CLI has been reworked in 2.x")
public class QuarkusCliClientIT {

    static final String RESTEASY_REACTIVE_EXTENSION = "quarkus-resteasy-reactive";
    static final String SMALLRYE_HEALTH_EXTENSION = "quarkus-smallrye-health";
    static final int CMD_DELAY_SEC = 3;
    // TODO: we only specify stream till first Quarkus 3 final is released to avoid javax/jakarta conflicts
    private static final String STREAM_VERSION = "3.0";

    @Inject
    static QuarkusCliClient cliClient;

    @Test
    public void shouldCreateApplicationOnJvm() {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app",
                QuarkusCliClient.CreateApplicationRequest.defaults().withStream(STREAM_VERSION));

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
                QuarkusCliClient.CreateExtensionRequest.defaults().withStream(STREAM_VERSION));

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The extension build failed. Output: " + result.getOutput());
    }

    @Test
    public void shouldCreateApplicationUsingArtifactId() {
        QuarkusCliRestService app = cliClient.createApplication("com.mycompany:my-app",
                QuarkusCliClient.CreateApplicationRequest.defaults().withStream(STREAM_VERSION));
        assertEquals("my-app", app.getServiceFolder().getFileName().toString(), "The application directory differs.");

        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());
    }

    @Test
    public void shouldAddAndRemoveExtensions() throws InterruptedException {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app",
                QuarkusCliClient.CreateApplicationRequest.defaults().withStream(STREAM_VERSION));

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
}
