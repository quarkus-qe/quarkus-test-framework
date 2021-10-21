package io.quarkus.test.bootstrap;

import static io.quarkus.test.utils.PropertiesUtils.SLASH;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

public class KeycloakService extends BaseService<KeycloakService> {

    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final int HTTP_80 = 80;

    private final List<String> realms = new ArrayList<>();

    public KeycloakService(String file, String realm) {
        this(realm);
        withProperty("KEYCLOAK_IMPORT", "resource::" + file);
    }

    public KeycloakService(List<String> realms, String realmsFolderPath) {
        this.realms.addAll(realms);
        if (realmsFolderPath.startsWith(SLASH)) {
            realmsFolderPath = realmsFolderPath.replaceFirst(SLASH, StringUtils.EMPTY);
        }

        if (!isValidRealmFolder(realmsFolderPath)) {
            throw new RuntimeException(String.format("Unexpected realms folder %s ", realmsFolderPath));
        }

        withProperty("KEYCLOAK_OCP_FOLDER", "resource:://" + realmsFolderPath);
        withProperty("JAVA_OPTS_APPEND", "-Dkeycloak.migration.action=import "
                + "-Dkeycloak.migration.provider=dir "
                + "-Dkeycloak.migration.dir=/" + realmsFolderPath + " "
                + "-Dkeycloak.migration.strategy=OVERWRITE_EXISTING ");
        withProperty("KEYCLOAK_USER", USER);
        withProperty("KEYCLOAK_PASSWORD", PASSWORD);
    }

    public KeycloakService(String realm) {
        this.realms.add(realm);
        withProperty("KEYCLOAK_USER", USER);
        withProperty("KEYCLOAK_PASSWORD", PASSWORD);
    }

    public String getRealmUrl(String realm) {
        String url = getHost();
        // SMELL: Keycloak does not validate Token Issuers when URL contains the port 80.
        if (getPort() != HTTP_80) {
            url += ":" + getPort();
        }

        return String.format("%s/auth/realms/%s", url, realm);
    }

    public AuthzClient createAuthzClient(String clientId, String clientSecret) {
        String realm = getMainRealm();
        return authzClientInstance(realm, clientId, clientSecret);
    }

    public AuthzClient createAuthzClient(String realm, String clientId, String clientSecret) {
        if (!realms.contains(realm)) {
            throw new RuntimeException(String.format("Realm %s not found", realm));
        }

        return authzClientInstance(realm, clientId, clientSecret);
    }

    private AuthzClient authzClientInstance(String realm, String clientId, String clientSecret) {
        return AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getRealmUrl(realm), "/realms"),
                realm,
                clientId,
                Collections.singletonMap("secret", clientSecret),
                HttpClients.createDefault()));
    }

    private String getMainRealm() {
        return realms.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unexpected empty realms"));
    }

    private boolean isValidRealmFolder(String realmsFolderPath) {
        ClassLoader classLoader = getClass().getClassLoader();
        File realmsFolder = new File(classLoader.getResource(realmsFolderPath).getFile());
        String[] realmConfigFiles = filterRealmConfigFiles(realmsFolder);
        return realmsFolder.isDirectory() && realmConfigFiles.length > 0;
    }

    private String[] filterRealmConfigFiles(File realmsFolderPath) {
        return Objects.requireNonNull(realmsFolderPath.list((dir, name) -> name.contains(".json")));
    }
}
