package io.quarkus.qe;

import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_BASE_PATH;
import static io.quarkus.test.bootstrap.KeycloakService.DEFAULT_REALM_FILE;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.KeycloakContainer;

public abstract class BaseSecurityResourceIT {

    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";
    static final String NORMAL_USER = "test-normal-user";

    @KeycloakContainer(command = { "start-dev", "--import-realm" })
    static KeycloakService keycloak = new KeycloakService(DEFAULT_REALM_FILE, DEFAULT_REALM, DEFAULT_REALM_BASE_PATH);

    private AuthzClient authzClient;

    protected abstract RestService getApp();

    @BeforeEach
    public void setup() {
        initAuthzClient();
    }

    @Test
    public void checkUserResourceByNormalUser() {
        getApp().given()
                .auth().oauth2(getTokenByTestNormalUser())
                .get("/user")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-normal-user"));
    }

    private void initAuthzClient() {
        authzClient = keycloak.createAuthzClient(CLIENT_ID_DEFAULT, CLIENT_SECRET_DEFAULT);
    }

    private String getTokenByTestNormalUser() {
        return authzClient.obtainAccessToken(NORMAL_USER, NORMAL_USER).getToken();
    }
}
