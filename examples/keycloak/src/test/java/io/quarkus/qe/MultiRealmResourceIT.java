package io.quarkus.qe;

import java.util.List;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class MultiRealmResourceIT extends BaseSecurityResourceIT {
    static final String SELECTED_REALM = "test-two-realm";
    static final List<String> DEFAULT_REALMS = List.of("test-realm", SELECTED_REALM);
    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";

    @Container(image = "quay.io/keycloak/keycloak:14.0.0", expectedLog = "Admin console listening", port = 8080)
    static final KeycloakService keycloak = new KeycloakService(DEFAULT_REALMS, "/realms");

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl(SELECTED_REALM))
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

    @Override
    protected void initAuthzClient() {
        authzClient = getKeycloak().createAuthzClient(SELECTED_REALM, CLIENT_ID_DEFAULT, CLIENT_SECRET_DEFAULT);
    }
}
