package io.quarkus.qe;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.Container;

public abstract class BaseSecurityResourceIT {

    static final String REALM_DEFAULT = "test-realm";
    static final String CLIENT_ID_DEFAULT = "test-application-client";
    static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";
    static final String NORMAL_USER = "test-normal-user";

    @Container(image = "quay.io/keycloak/keycloak:11.0.3", expectedLog = "Admin console listening", port = 8080)
    static final KeycloakService keycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT);

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
