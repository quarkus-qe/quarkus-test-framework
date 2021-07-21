package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
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

    @Disabled("TODO: Fix flaky test: https://github.com/quarkus-qe/quarkus-test-framework/issues/146")
    @Test
    public void shouldDetectNewTests() {
        // At first, there are no tests annotated with @QuarkusTest
        app.logs().assertContains("Tests paused");
        // Now, we enable continuous testing via DEV UI
        app.enableContinuousTesting();
        // We add a new test
        app.copyFile("src/test/resources/GreetingResourceTest.java.template", "src/test/java/GreetingResourceTest.java");
        // So good so far!
        app.logs().assertContains("All 1 test is passing");
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
