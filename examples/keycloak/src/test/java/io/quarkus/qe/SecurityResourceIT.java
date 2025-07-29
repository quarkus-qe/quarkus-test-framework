package io.quarkus.qe;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class SecurityResourceIT extends BaseSecurityResourceIT {

    @KeycloakContainer(runKeycloakInProdMode = true)
    static final KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", keycloak::getRealmUrl)
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT)
            .withProperties(keycloak::getTlsProperties);

    @Override
    public RestService getApp() {
        return app;
    }
}
