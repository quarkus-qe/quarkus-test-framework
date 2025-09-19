package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.qe.sources.AppleResource;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class ClassLoadingIT {

    /**
     * Test that all classes from src/main are include, while additional class from src/test is also included.
     */
    @QuarkusApplication(includeAllClassesFromMain = true, classes = { AppleResource.class })
    static final RestService app = new RestService()
            .withProperty(ValidateCustomProperty.CUSTOM_PROPERTY, "Frodo");

    @Test
    public void testEndpoints() {
        // check resource from src/main
        app.given().get("/greeting").then()
                .statusCode(200)
                .body(is("Hello, I'm Frodo"));

        // check resource from src/test
        app.given().get("/apple").then()
                .statusCode(200)
                .body(is("Hello, I'm an apple"));
    }
}
