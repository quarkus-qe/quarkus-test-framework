package io.quarkus.test.bootstrap;

import java.net.URI;
import java.net.URISyntaxException;
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
        var host = getURI(Protocol.HTTP);

        // SMELL: Keycloak does not validate Token Issuers when URL contains the port 80.
        int port = host.getPort();
        if (port == HTTP_80) {
            port = -1;
        }
        try {
            URI url = new URI(host.getScheme(),
                    host.getUserInfo(),
                    host.getHost(),
                    port,
                    "/auth/realms/" + realm,
                    null,
                    null);
            return url.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
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
