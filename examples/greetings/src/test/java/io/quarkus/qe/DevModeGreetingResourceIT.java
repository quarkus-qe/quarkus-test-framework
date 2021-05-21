package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@DisabledOnNative
@DisabledOnQuarkusVersion(version = "1\\..*", reason = "Continuous Testing was entered in 2.x")
@QuarkusScenario
public class DevModeGreetingResourceIT {

    static final String VICTOR_NAME = "victor";

    static final String HELLO_IN_ENGLISH = "Hello";
    static final String HELLO_IN_SPANISH = "Hola";

    @DevModeQuarkusApplication
    static DevModeQuarkusService app = new DevModeQuarkusService();

    @Test
    public void shouldDetectNewTests() {
        // At first, there are no tests annotated with @QuarkusTest
        app.logs().assertContains("Tests paused, press [r] to resume");
        // Now, we enable continuous testing via DEV UI
        app.enableContinuousTesting();
        // We wait for Quarkus to run the tests
        app.logs().assertContains("Running Tests for the first time");
        // But there are no tests yet
        app.logs().assertContains("No tests to run");
        // We add a new test
        app.copyFile("src/test/resources/GreetingResourceTest.java.template", "src/test/java/GreetingResourceTest.java");
        // And now, Quarkus find it and run it
        app.logs().assertContains("Running 0/1");
        // So good so far!
        app.logs().assertContains("All 1 tests are passing");
    }

    @Test
    public void shouldUpdateResourcesAndSources() {
        // Should say first Victor (the default name)
        app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is(HELLO_IN_ENGLISH + ", I'm " + VICTOR_NAME));

        // Modify default name to manuel
        app.modifyFile("src/main/java/io/quarkus/qe/GreetingResource.java",
                content -> content.replace(HELLO_IN_ENGLISH, HELLO_IN_SPANISH));

        // Now, the app should say Manuel
        AwaitilityUtils.untilAsserted(
                () -> app.given().get("/greeting").then().statusCode(HttpStatus.SC_OK)
                        .body(is(HELLO_IN_SPANISH + ", I'm " + VICTOR_NAME)));
    }
}
