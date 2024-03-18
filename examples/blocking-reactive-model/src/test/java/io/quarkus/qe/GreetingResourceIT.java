package io.quarkus.qe;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private static final String REST = "rest";

    @QuarkusApplication(dependencies = @Dependency(artifactId = "quarkus-" + RESTEASY_CLASSIC))
    static final RestService resteasyClassic = new RestService();

    @QuarkusApplication(dependencies = @Dependency(artifactId = "quarkus-" + REST))
    static final RestService rest = new RestService();

    @Test
    public void shouldPickTheForcedDependencies() {
        // resteasy classic
        resteasyClassic.given().get(HELLO_PATH).then().body(is(HELLO));
        assertTrue(resteasyClassic.logs().forQuarkus().installedFeatures().contains(RESTEASY_CLASSIC));

        // rest
        rest.given().get(HELLO_PATH).then().body(is(HELLO));
        assertTrue(rest.logs().forQuarkus().installedFeatures().contains(REST));
        assertFalse(rest.logs().forQuarkus().installedFeatures().contains(RESTEASY_CLASSIC));
    }
}
