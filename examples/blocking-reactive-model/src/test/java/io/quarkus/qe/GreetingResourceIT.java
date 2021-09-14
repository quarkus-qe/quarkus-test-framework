package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class GreetingResourceIT {

    private static final String HELLO = "Hello";
    private static final String HELLO_PATH = "/hello";
    private static final String RESTEASY_CLASSIC = "resteasy";
    private static final String RESTEASY_REACTIVE = "resteasy-reactive";

    @QuarkusApplication(dependencies = @Dependency(artifactId = "quarkus-" + RESTEASY_CLASSIC))
    static final RestService blocking = new RestService();

    @QuarkusApplication(dependencies = @Dependency(artifactId = "quarkus-" + RESTEASY_REACTIVE))
    static final RestService reactive = new RestService();

    @Test
    public void shouldPickTheForcedDependencies() {
        // classic
        blocking.given().get(HELLO_PATH).then().body(is(HELLO));
        blocking.logs().forQuarkus().installedFeatures().contains(RESTEASY_CLASSIC);

        // reactive
        reactive.given().get(HELLO_PATH).then().body(is(HELLO));
        blocking.logs().forQuarkus().installedFeatures().contains(RESTEASY_REACTIVE);
    }
}
