package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class VerifyNonAppEndpointsIT {

    public static final List<String> NON_APP_ENDPOINTS = Arrays.asList(
            "/openapi", "/metrics/base", "/metrics/application",
            "/metrics/vendor", "/metrics", "/health/group", "/health/well", "/health/ready",
            "/health/live", "/health");

    @QuarkusApplication
    static final RestService app = new RestService();

    @Test
    public void verifyNonAppRootPathIsWorking() {
        givenRootPath("/api");
        givenNonAppRootPath("/q");
        givenNonAppRootRedirectedIsDisabled();
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeOk("/q");
    }

    @Test
    public void verifyNonAppRootPathIsWorkingWhenRootPathChanged() {
        givenRootPath("/");
        givenNonAppRootPath("/q");
        givenNonAppRootRedirectedIsDisabled();
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeOk("/q");
    }

    @Test
    public void verifyNonAppRootPathIsNotRedirected() {
        givenRootPath("/api");
        givenNonAppRootPath("/");
        givenNonAppRootRedirectedIsDisabled();
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeNotFound("/api");
    }

    @Test
    public void verifyNonAppRootPathIsRedirected() {
        givenRootPath("/api");
        givenNonAppRootPath("/q");
        givenNonAppRootRedirectedIsEnabled();
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeRedirected("/api");
    }

    private void givenNonAppRootPath(String path) {
        app.withProperty("quarkus.http.non-application-root-path", path);
    }

    private void givenRootPath(String path) {
        app.withProperty("quarkus.http.root-path", path);
    }

    private void givenNonAppRootRedirectedIsEnabled() {
        givenNonAppRootRedirectedIs(true);
    }

    private void givenNonAppRootRedirectedIsDisabled() {
        givenNonAppRootRedirectedIs(false);
    }

    private void givenNonAppRootRedirectedIs(boolean flag) {
        app.withProperty("quarkus.http.redirect-to-non-application-root-path", "" + flag);
    }

    private void whenUpdateProperties() {
        app.restart();
    }

    private void thenNonAppEndpointsShouldBeRedirected(String basePath) {
        for (String endpoint : NON_APP_ENDPOINTS) {
            await().untilAsserted(() -> {
                int actual = app.given().redirects().follow(false).get(basePath + endpoint).getStatusCode();
                assertEquals(HttpStatus.SC_MOVED_PERMANENTLY, actual, "Endpoint '" + endpoint + "' is not redirected");
            });
        }
    }

    private void thenNonAppEndpointsShouldBeOk(String basePath) {
        thenNonAppEndpointsShouldBe(basePath, HttpStatus.SC_OK);
    }

    private void thenNonAppEndpointsShouldBeNotFound(String basePath) {
        thenNonAppEndpointsShouldBe(basePath, HttpStatus.SC_NOT_FOUND);
    }

    private void thenNonAppEndpointsShouldBe(String basePath, int status) {
        for (String endpoint : NON_APP_ENDPOINTS) {
            await().untilAsserted(() -> {
                int actual = app.given().get(basePath + endpoint).getStatusCode();
                assertEquals(status, actual, "Endpoint '" + endpoint + "' with not expected status " + actual);
            });
        }
    }

    private ConditionFactory await() {
        return Awaitility.await().ignoreExceptions()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS);
    }

}
