package io.quarkus.test.bootstrap;

public class KeycloakService extends BaseService<KeycloakService> {

    private static final String USER = "admin";
    private static final String PASSWORD = "admin";

    private final String realm;

    public KeycloakService(String file, String realm) {
        this.realm = realm;
        withProperty("KEYCLOAK_USER", USER);
        withProperty("KEYCLOAK_PASSWORD", PASSWORD);
        withProperty("KEYCLOAK_IMPORT", "resource::" + file);
    }

    public String getRealmUrl() {
        return String.format("%s:%s/auth/realms/%s", getHost(), getPort(), realm);
    }
}
