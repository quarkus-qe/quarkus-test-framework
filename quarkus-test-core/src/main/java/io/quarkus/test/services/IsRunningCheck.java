package io.quarkus.test.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.logging.Log;
import io.restassured.RestAssured;

/**
 * A check that can mark the resource as running. All implementors must have single public no-args constructor.
 */
public interface IsRunningCheck {

    boolean isRunning(IsRunningCheckContext context);

    interface IsRunningCheckContext {
        URILike getURI(Protocol protocol);
    }

    final class AlwaysFail implements IsRunningCheck {

        public AlwaysFail() {
        }

        @Override
        public boolean isRunning(IsRunningCheckContext context) {
            return false;
        }
    }

    /**
     * The Quarkus QE Framework will treat this service as 'running' if
     * and only if request to given path responds with 4xx, 3xx or 2xx response code.
     * Please use this option with caution because result is intentionally not cached (it would be complex for DEV mode
     * scenarios) and the request could be executed as often as every time
     * the {@link io.quarkus.test.bootstrap.BaseService#isRunning()} is called.
     */
    abstract class IsPathReachableCheck implements IsRunningCheck {

        static final String BASE_PATH = "/";
        private final Protocol protocol;
        private final String path;
        private final String body;

        protected IsPathReachableCheck(Protocol protocol, String path) {
            this(protocol, path, null);
        }

        protected IsPathReachableCheck(Protocol protocol, String path, String body) {
            this.protocol = protocol;
            this.path = path;
            this.body = body;
        }

        @Override
        public boolean isRunning(IsRunningCheckContext context) {
            try {
                var host = context.getURI(protocol);
                var response = RestAssured.given()
                        .baseUri(host.getRestAssuredStyleUri())
                        .basePath(BASE_PATH)
                        .port(host.getPort())
                        .get(path)
                        .then()
                        .statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(HttpStatus.SC_OK),
                                Matchers.lessThan(HttpStatus.SC_INTERNAL_SERVER_ERROR)))
                        .extract();
                if (body != null) {
                    String actualBody = response.body().asString();
                    assertNotNull(actualBody);
                    assertTrue(actualBody.contains(body),
                            () -> "Expected response body to contain '%s', but got '%s'".formatted(body, actualBody));
                }
                int statusCode = response.statusCode();
                Log.debug("Readiness check for path '%s' and protocol '%s' passed with response status '%s'", path, protocol,
                        statusCode);
            } catch (Throwable t) {
                Log.debug("Service is not yet ready: ", t);
                return false;
            }
            return true;
        }
    }

    /**
     * This is {@link IsPathReachableCheck} for the "/" path and the {@link Protocol#HTTP} protocol.
     *
     * @see IsPathReachableCheck for more information
     */
    final class IsBasePathReachableCheck extends IsPathReachableCheck {

        public IsBasePathReachableCheck() {
            super(Protocol.HTTP, IsPathReachableCheck.BASE_PATH);
        }
    }
}
