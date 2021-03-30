package io.quarkus.qe;

import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
public class SecurityResourceIT {

    private static final String REALM_DEFAULT = "test-realm";
    private static final String CLIENT_ID_DEFAULT = "test-application-client";
    private static final String CLIENT_SECRET_DEFAULT = "test-application-client-secret";
    private static final String NORMAL_USER = "test-normal-user";

    @Container(image = "quay.io/keycloak/keycloak:11.0.3", expectedLog = "Admin console listening", port = 8080)
    static final KeycloakService keycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT);

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.oidc.auth-server-url", () -> keycloak.getRealmUrl())
            .withProperty("quarkus.oidc.client-id", CLIENT_ID_DEFAULT)
            .withProperty("quarkus.oidc.credentials.secret", CLIENT_SECRET_DEFAULT);

    private AuthzClient authzClient;

    @BeforeEach
    public void setup() {
        initAuthzClient();
    }

    @Test
    public void checkUserResourceByNormalUser() {
        app.given()
                .auth().oauth2(getTokenByTestNormalUser())
                .get("/user")
                .then()
                .statusCode(200)
                .body(equalTo("Hello, user test-normal-user"));
    }

    private void initAuthzClient() {
        authzClient = AuthzClient.create(new Configuration(
                StringUtils.substringBefore(keycloak.getRealmUrl(), "/realms"),
                REALM_DEFAULT,
                CLIENT_ID_DEFAULT,
                Collections.singletonMap("secret", CLIENT_SECRET_DEFAULT),
                HttpClients.createDefault()));
    }

    private String getTokenByTestNormalUser() {
        return authzClient.obtainAccessToken(NORMAL_USER, NORMAL_USER).getToken();
    }
}
