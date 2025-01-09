package io.quarkus.test.bootstrap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

public class KeycloakService extends BaseService<KeycloakService> {

    public static final String DEFAULT_REALM_BASE_PATH = "/realms";
    public static final String DEFAULT_REALM = "test-realm";
    // The convention for importing realm is to have file name like `<realm-name>-realm.json`
    public static final String DEFAULT_REALM_FILE = "/test-realm-realm.json";
    private static final String REALM_DEST_PATH = "/opt/keycloak/data/import";
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final int HTTP_80 = 80;

    private String realmBasePath = "realms";
    private final String realm;

    /**
     * KeycloakService constructor, supported since Keycloak 18.
     *
     * @param realmFile for example /test-realm-realm.json
     * @param realmName
     * @param realmBasePath such as "/realms" used by Keycloak 18 or "auth/realms" used by previous versions
     */
    public KeycloakService(String realmFile, String realmName, String realmBasePath) {
        this(realmName);
        this.realmBasePath = normalizeRealmBasePath(realmBasePath);
        withProperty("KEYCLOAK_REALM_IMPORT", "resource_with_destination::" + REALM_DEST_PATH + "|" + realmFile);
    }

    public KeycloakService(String realmName) {
        this.realm = realmName;
        withProperty("KC_BOOTSTRAP_ADMIN_USERNAME", USER);
        withProperty("KC_BOOTSTRAP_ADMIN_PASSWORD", PASSWORD);
        // TODO drop next variables as they were deprecated in KC 26 (possibly when we move to KC 28+)
        withProperty("KEYCLOAK_ADMIN", USER);
        withProperty("KEYCLOAK_ADMIN_PASSWORD", PASSWORD);
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
