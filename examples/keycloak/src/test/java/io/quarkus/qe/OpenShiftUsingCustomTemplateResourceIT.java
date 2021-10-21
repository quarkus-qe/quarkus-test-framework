package io.quarkus.qe;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftUsingCustomTemplateResourceIT {

    private static final String REALM_DEFAULT = "test-realm";
    private static final String CLIENT_ID_DEFAULT = "test-application-client";
    private static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    @Container(image = "quay.io/keycloak/keycloak:14.0.0", expectedLog = "Admin console listening", port = 8080)
    static final KeycloakService customkeycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT);

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> customkeycloak.getRealmUrl(REALM_DEFAULT))
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
