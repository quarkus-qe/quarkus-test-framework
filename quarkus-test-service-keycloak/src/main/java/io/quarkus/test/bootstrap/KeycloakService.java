package io.quarkus.test.bootstrap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

public class KeycloakService extends BaseService<KeycloakService> {

    private static final String REALM_DEST_PATH = "/opt/keycloak/data/import";
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final int HTTP_80 = 80;

    private String realmBasePath = "realms";
    private final String realm;

    /**
     * KeycloakService constructor, supported since Keycloak 18.
     *
     * @param realmFile for example /keycloak-realm.json
     * @param realmName
     * @param realmBasePath such as "/realms" used by Keycloak 18 or "auth/realms" used by previous versions
     */
    public KeycloakService(String realmFile, String realmName, String realmBasePath) {
        this(realmName);
        this.realmBasePath = normalizeRealmBasePath(realmBasePath);
        withProperty("KEYCLOAK_IMPORT", "resource::" + realmFile); // Required by keycloak 16 and lower
        withProperty("KEYCLOAK_REALM_IMPORT", "resource_with_destination::" + REALM_DEST_PATH + "|" + realmFile);
    }

    /**
     * Legacy constructor used by previous versions of Keycloak 18.
     */
    @Deprecated
    public KeycloakService(String realmFile, String realmName) {
        this(realmName);
        withProperty("KEYCLOAK_IMPORT", "resource::" + realmFile);
        withProperty("KEYCLOAK_REALM_IMPORT", "resource_with_destination::" + REALM_DEST_PATH + "|" + realmFile);
    }

    public KeycloakService(String realmName) {
        this.realm = realmName;
        withProperty("KEYCLOAK_ADMIN", USER);
        withProperty("KEYCLOAK_ADMIN_PASSWORD", PASSWORD);
        withProperty("KEYCLOAK_USER", USER); // Required by keycloak 16 and lower
        withProperty("KEYCLOAK_PASSWORD", PASSWORD); // Required by keycloak 16 and lower
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
                    "/" + realmBasePath + "/" + realm,
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

    private String normalizeRealmBasePath(String realmBasePath) {
        if (realmBasePath.startsWith("/")) {
            realmBasePath = realmBasePath.substring(1);
        }

        if (realmBasePath.endsWith("/")) {
            realmBasePath = realmBasePath.substring(0, realmBasePath.length() - 1);
        }

        return realmBasePath;
    }
}
