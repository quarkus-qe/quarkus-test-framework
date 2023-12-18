package io.quarkus.qe;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftUsingCustomTemplateResourceIT {

    private static final String CLIENT_ID_DEFAULT = "test-application-client";
    private static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    @Container(image = "quay.io/keycloak/keycloak:22.0.1", expectedLog = "started", port = 8080)
    static final KeycloakService customkeycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM,
            DEFAULT_REALM_BASE_PATH);

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", customkeycloak::getRealmUrl)
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT);

    @Test
    public void checkUserResourceByNormalUser() {
        app.given()
                .get("/user")
                .then()
                .statusCode(401);
    }
}
