package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshot;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.services.QuarkusApplication;

@DisabledOnNative(reason = "Due to high native build execution time in every restart")
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
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeOk("/q");
    }

    @Test
    public void verifyNonAppRootPathIsWorkingWhenRootPathChanged() {
        givenRootPath("/");
        givenNonAppRootPath("/q");
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeOk("/q");
    }

    @Test
    public void verifyNonAppRootPathIsNotRedirected() {
        givenRootPath("/api");
        givenNonAppRootPath("/");
        whenUpdateProperties();
        thenNonAppEndpointsShouldBeNotFound("/api");
    }

    @DisabledOnQuarkusVersion(version = "2\\..*", reason = "Redirection is no longer supported in 2.x")
    @DisabledOnQuarkusSnapshot(reason = "Redirection is no longer supported in 999-SNAPSHOT")
    @Test
    public void verifyNonAppRootPathIsRedirected() {
        givenRootPath("/api");
        givenNonAppRootPath("/q");
    }

    private void givenNonAppRootPath(String path) {
        app.withProperty("quarkus.http.non-application-root-path", path);
    }

    private void givenRootPath(String path) {
        app.withProperty("quarkus.http.root-path", path);
    }

    private void whenUpdateProperties() {
        app.restart();
    }

    private void thenNonAppEndpointsShouldBeOk(String basePath) {
        thenNonAppEndpointsShouldBe(basePath, HttpStatus.SC_OK);
    }

    private void thenNonAppEndpointsShouldBeNotFound(String basePath) {
        thenNonAppEndpointsShouldBe(basePath, HttpStatus.SC_NOT_FOUND);
    }

    private void thenNonAppEndpointsShouldBe(String basePath, int status) {
        for (String endpoint : NON_APP_ENDPOINTS) {
            untilAsserted(() -> app.given().get(basePath + endpoint).getStatusCode(),
                    actual -> assertEquals(status, actual, "Endpoint '" + endpoint + "' with not expected status " + actual));
        }
    }

}
