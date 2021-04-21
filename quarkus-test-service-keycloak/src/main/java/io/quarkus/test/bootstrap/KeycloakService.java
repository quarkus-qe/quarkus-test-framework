package io.quarkus.test.bootstrap;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

public class KeycloakService extends BaseService<KeycloakService> {

    private static final String USER = "admin";
    private static final String PASSWORD = "admin";

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
        return String.format("%s:%s/auth/realms/%s", getHost(), getPort(), realm);
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
