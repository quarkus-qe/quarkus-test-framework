package io.quarkus.test.bootstrap;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

public class KeycloakService extends BaseService<KeycloakService> {

    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final int HTTP_80 = 80;

    private final String realm;

    public KeycloakService(String file, String realm) {
        this(realm);
        withProperty("KEYCLOAK_IMPORT", "resource::" + file);
    }

    public KeycloakService(String realm) {
        this.realm = realm;
        withProperty("KEYCLOAK_USER", USER);
        withProperty("KEYCLOAK_PASSWORD", PASSWORD);
    }

    public String getRealmUrl() {
        String url = getHost();
        // SMELL: Keycloak does not validate Token Issuers when URL contains the port 80.
        if (getPort() != HTTP_80) {
            url += ":" + getPort();
        }

        return String.format("%s/auth/realms/%s", url, realm);
    }

    public AuthzClient createAuthzClient(String clientId, String clientSecret) {
        return AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getRealmUrl(), "/realms"),
                realm,
                clientId,
                Collections.singletonMap("secret", clientSecret),
                HttpClients.createDefault()));
    }
}
