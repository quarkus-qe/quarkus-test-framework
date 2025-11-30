package io.quarkus.qe;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
public class OpenShiftServerlessUsingExtensionGreetingResourceLogIT {

    static final String JOSE_NAME = "jose";
    static final String MANUEL_NAME = "manuel";

    @QuarkusApplication
    static final RestService joseApp = new RestService().withProperties("jose.properties");
    @QuarkusApplication
    static final RestService manuelApp = new RestService().withProperties("manuel.properties");

    @Test
    public void verifyLogsAndConfigForJose() {
        verifyApp(joseApp, JOSE_NAME);
    }

    @Test
    public void verifyLogsAndConfigForManuel() {
        verifyApp(manuelApp, MANUEL_NAME);
    }

    private void verifyApp(RestService app, String expectedName) {
        app.given()
                .relaxedHTTPSValidation()
                .get("/greeting")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("Hello, I'm " + expectedName));

        assertNotEquals(0, app.getLogs().size(), "App logs are empty!");

        String expectedLogMessage = String.format("App started with custom property: %s", expectedName);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> logs = app.getLogs();
            boolean messageFound = logs.stream()
                    .anyMatch(line -> line.contains(expectedLogMessage));
            assertTrue(messageFound,
                    "Logs about custom property config are missing, full logs: " + logs);
        });
    }
}
