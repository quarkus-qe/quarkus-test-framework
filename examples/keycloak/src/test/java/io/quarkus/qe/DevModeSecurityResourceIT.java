package io.quarkus.qe;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.DevModeQuarkusApplication;

@QuarkusScenario
public class DevModeSecurityResourceIT extends BaseSecurityResourceIT {

    @Container(image = "quay.io/keycloak/keycloak:14.0.0", expectedLog = "Admin console listening", port = 8080)
    static final KeycloakService keycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT);

    @DevModeQuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl(REALM_DEFAULT))
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT);

    @Override
    public RestService getApp() {
        return app;
    }

    @Override
    protected KeycloakService getKeycloak() {
        return keycloak;
    }
}
