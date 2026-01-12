package io.quarkus.qe;

import static io.quarkus.test.services.quarkus.RemoteDevModeQuarkusApplicationManagedResourceBuilder.EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.DevModeQuarkusService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.logging.Log;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.IsRunningCheck;
import io.quarkus.test.services.RemoteDevModeQuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

@QuarkusScenario
public class RemoteDevModeGreetingResourceIT {

    static final String VICTOR_NAME = "victor";

    static final String HELLO_IN_ENGLISH = "Hello";
    static final String HELLO_IN_SPANISH = "Hola";

    static final String EXPECTED_OUTPUT_REMOTE_DEV_REQ_FAILED = "Remote dev request failed";

    @RemoteDevModeQuarkusApplication(isRunningCheck = IsGreetingPathReachableCheck.class)
    static final DevModeQuarkusService app = new DevModeQuarkusService();

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

    public static final class IsGreetingPathReachableCheck extends IsRunningCheck.IsPathReachableCheck {

        public IsGreetingPathReachableCheck() {
            super(Protocol.HTTP, "/greeting", VICTOR_NAME);
        }

        @Override
        public boolean isRunning(IsRunningCheckContext context) {
            if (super.isRunning(context)) {
                // io.quarkus.test.services.quarkus.RemoteDevModeLocalhostQuarkusApplicationManagedResource.isRunning
                // does this by default, it is the right thing to do
                if (app.getLogs().stream().anyMatch(l -> l.contains(EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON))) {
                    Log.debug("Logs contain expected output '%s'", EXPECTED_OUTPUT_FROM_REMOTE_DEV_DAEMON);
                    return true;
                }
                // but if it didn't work, there are still legit situations when this output is not logged, but
                // Quarkus can still recover: https://github.com/quarkusio/quarkus/issues/48198#issuecomment-2939698255
                // so here, we will just trust the IsPathReachableCheck result
                if (app.getLogs().stream().anyMatch(l -> l.contains(EXPECTED_OUTPUT_REMOTE_DEV_REQ_FAILED))) {
                    Log.debug("Logs contain expected output '%s'", EXPECTED_OUTPUT_REMOTE_DEV_REQ_FAILED);
                    return true;
                }
            }
            return false;

        }
    }
}
