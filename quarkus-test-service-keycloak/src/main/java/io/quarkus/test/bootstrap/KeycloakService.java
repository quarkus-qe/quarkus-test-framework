package io.quarkus.test.bootstrap;

import static io.quarkus.test.services.containers.KeycloakContainerManagedResourceBuilder.CERTIFICATE_CONTEXT_KEY;
import static io.quarkus.test.services.containers.KeycloakContainerManagedResourceBuilder.KEYCLOAK_PRODUCTION_MODE_KEY;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_PREFIX;
import static io.quarkus.test.utils.TestExecutionProperties.isBareMetalPlatform;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.security.certificate.Certificate;

public class KeycloakService extends BaseService<KeycloakService> {

    public static final String DEFAULT_REALM_BASE_PATH = "/realms";
    public static final String DEFAULT_REALM = "test-realm";
    public static final String KEYSTORE_PASSWORD = "secret";
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
        withProperty("KEYCLOAK_REALM_IMPORT", RESOURCE_WITH_DESTINATION_PREFIX + REALM_DEST_PATH + "|" + realmFile);
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
        boolean runKeycloakInProdMode = getPropertyFromContext(KEYCLOAK_PRODUCTION_MODE_KEY);
        var host = runKeycloakInProdMode ? getURI(Protocol.HTTPS) : getURI(Protocol.HTTP);

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
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        try {
            // Needed to disable the cert check as default setup not allowing self-sign certificates
            TrustStrategy trustStrategy = (cert, authType) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new IllegalStateException("Unable to create SSLConnectionSocketFactory to allow"
                    + " secured connection use self-signed certificates", e);
        }
        return AuthzClient.create(new Configuration(
                StringUtils.substringBefore(getRealmUrl(), "/realms"),
                realm,
                clientId,
                Collections.singletonMap("secret", clientSecret),
                HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build()));
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

    public String getTrustStore() {
        Certificate certBuilder = getPropertyFromContext(CERTIFICATE_CONTEXT_KEY);
        if (certBuilder == null) {
            throw new IllegalArgumentException("Unable to load CertificateBuilder.");
        }

        String trustStore = certBuilder.truststorePath();
        if (isBareMetalPlatform()) {
            return trustStore;
        } else {
            var deploymentStrategy = context.getScenarioContext().getAnnotation(OpenShiftScenario.class).deployment();
            if (deploymentStrategy.equals(OpenShiftDeploymentStrategy.Build)
                    || deploymentStrategy.equals(OpenShiftDeploymentStrategy.UsingContainerRegistry)) {
                return SECRET_PREFIX + Path.of(trustStore).getFileName().toString();
            }
            // Don't need to mount it to openshift for UsingOpenShiftExtensionAndDockerBuildStrategy and UsingOpenShiftExtension
            // strategies as the key was copied to be present when building app
            // as part of KeycloakContainerManagedResourceBuilder#setUpProdKeycloak
            return Path.of(trustStore).getFileName().toString();
        }
    }

    public Map<String, String> getTlsProperties() {
        Certificate cert = getPropertyFromContext(CERTIFICATE_CONTEXT_KEY);
        Map<String, String> properties = new HashMap<>();
        if (cert != null) {
            final String additionalJdbcProperties = "quarkus.tls.oidc.trust-store.p12.";
            properties.put("quarkus.oidc.tls.tls-configuration-name", "oidc");
            properties.putAll(prepareTruststorePathProperty(cert));
            return properties;
        }
        return properties;
    }

    private Map<String, String> prepareTruststorePathProperty(Certificate cert) {
        final String tlsOidcPropertiesBase = "quarkus.tls.oidc.trust-store.";
        switch (cert.format()) {
            case "PEM" -> {
                return Map.of(
                        tlsOidcPropertiesBase + "pem.certs", getTrustStore());
            }
            case "PKCS12" -> {
                return Map.of(
                        tlsOidcPropertiesBase + "p12.path", getTrustStore(),
                        tlsOidcPropertiesBase + "p12.password", cert.password());
            }
            case "JKS" -> {
                return Map.of(
                        tlsOidcPropertiesBase + "jks.path", getTrustStore(),
                        tlsOidcPropertiesBase + "jks.password", cert.password());
            }
            default -> throw new IllegalArgumentException(cert.format() + " is not supported.");
        }
    }
}
