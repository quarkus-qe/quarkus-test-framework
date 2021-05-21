package io.quarkus.qe;

import static io.quarkus.test.utils.AwaitilityUtils.untilAsserted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
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

    @DisabledOnQuarkusVersion(version = "2\\..*", reason = "Redirection is no longer supported in 2.x")
    @DisabledOnQuarkusVersion(version = "999-SNAPSHOT", reason = "Redirection is no longer supported in 999-SNAPSHOT")
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
            untilAsserted(() -> app.given().redirects().follow(false).get(basePath + endpoint).getStatusCode(),
                    actual -> assertEquals(HttpStatus.SC_MOVED_PERMANENTLY, actual,
                            "Endpoint '" + endpoint + "' is not redirected"));
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
            untilAsserted(() -> app.given().get(basePath + endpoint).getStatusCode(),
                    actual -> assertEquals(status, actual, "Endpoint '" + endpoint + "' with not expected status " + actual));
        }
    }

}
