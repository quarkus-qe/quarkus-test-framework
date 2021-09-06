package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.services.RemoteDevModeQuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
@DisabledOnNative
public class RemoteDevGreetingResourceIT {

    static final String VICTOR_NAME = "victor";

    static final String HELLO_IN_ENGLISH = "Hello";
    static final String HELLO_IN_SPANISH = "Hola";

    @RemoteDevModeQuarkusApplication
    static DevModeQuarkusService app = new DevModeQuarkusService();

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

    @Test
    public void shouldLoadResources() {
        app.given().get("/greeting/file").then().statusCode(HttpStatus.SC_OK).body(is("found!"));
    }
}
