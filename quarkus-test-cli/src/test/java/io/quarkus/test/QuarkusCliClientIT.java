package io.quarkus.test;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.scenarios.annotations.EnabledOnNative;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;

@Tag("quarkus-cli")
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Quarkus CLI has been reworked in 2.x")
public class QuarkusCliClientIT {

    static final String RESTEASY_SPRING_WEB_EXTENSION = "quarkus-spring-web";
    static final String RESTEASY_EXTENSION = "quarkus-resteasy";
    static final String RESTEASY_JACKSON_EXTENSION = "quarkus-resteasy-jackson";
    static final String SMALLRYE_HEALTH_EXTENSION = "quarkus-smallrye-health";
    static final int CMD_DELAY_SEC = 3;

    @Inject
    static QuarkusCliClient cliClient;

    @Test
    public void shouldVersionMatchQuarkusVersion() {
        // Using option
        assertEquals(QuarkusProperties.getVersion(), cliClient.run("version").getOutput());

        // Using shortcut
        assertEquals(QuarkusProperties.getVersion(), cliClient.run("-v").getOutput());
    }

    @Test
    public void shouldCreateApplicationOnJvm() {
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
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows does not support Linux containers yet")
    public void shouldBuildApplicationOnNativeUsingDocker() {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // Should build on Native
        QuarkusCliClient.Result result = app.buildOnNative();
        assertTrue(result.isSuccessful(), "The application didn't build on Native. Output: " + result.getOutput());
    }

    @Test
    public void shouldCreateApplicationWithCodeStarter() {
        // Create application with Resteasy Jackson
        QuarkusCliRestService app = cliClient.createApplication("app",
                QuarkusCliClient.CreateApplicationRequest.defaults().withExtensions(RESTEASY_SPRING_WEB_EXTENSION,
                        RESTEASY_JACKSON_EXTENSION));

        // Verify By default, it installs only "quarkus-resteasy"
        assertInstalledExtensions(app, RESTEASY_SPRING_WEB_EXTENSION, RESTEASY_JACKSON_EXTENSION);

        // Start using DEV mode
        app.start();
        untilAsserted(() -> app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).and().body(is("Hello Spring")));
    }

    @Test
    public void shouldAddAndRemoveExtensions() throws InterruptedException {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // By default, it installs only "quarkus-resteasy"
        assertInstalledExtensions(app, RESTEASY_EXTENSION);

        // Let's install Quarkus Smallrye Health
        QuarkusCliClient.Result result = app.installExtension(SMALLRYE_HEALTH_EXTENSION);
        assertTrue(result.isSuccessful(), SMALLRYE_HEALTH_EXTENSION + " was not installed. Output: " + result.getOutput());

        // Verify both extensions now
        assertInstalledExtensions(app, RESTEASY_EXTENSION, SMALLRYE_HEALTH_EXTENSION);

        // The health endpoint should be now available
        app.start();
        untilAsserted(() -> app.given().get("/q/health").then().statusCode(HttpStatus.SC_OK));
        app.stop();

        // Let's now remove the Smallrye Health extension
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
