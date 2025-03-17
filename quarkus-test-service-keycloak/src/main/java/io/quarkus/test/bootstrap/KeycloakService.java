package io.quarkus.test.bootstrap;

import static io.quarkus.test.bootstrap.inject.OpenShiftClient.getOpenShiftUrl;
import static io.quarkus.test.utils.PropertiesUtils.RESOURCE_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.TestExecutionProperties.isBareMetalPlatform;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import io.smallrye.certs.CertificateFiles;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.smallrye.certs.JksCertificateFiles;
import io.smallrye.certs.PemCertificateFiles;
import io.smallrye.certs.Pkcs12CertificateFiles;

public class KeycloakService extends BaseService<KeycloakService> {

    public static final String DEFAULT_REALM_BASE_PATH = "/realms";
    public static final String DEFAULT_REALM = "test-realm";
    public static final String KEYSTORE_PASSWORD = "secret";
    // The convention for importing realm is to have file name like `<realm-name>-realm.json`
    public static final String DEFAULT_REALM_FILE = "/test-realm-realm.json";
    private static final String REALM_DEST_PATH = "/opt/keycloak/data/import";
    private static final String KEYSTORE_DEST_PATH = "/opt/keycloak/conf/";
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";
    private static final String KEYSTORE_PREFIX = "server";
    private static final Format DEFAULT_KEYSTORE_FORMAT = Format.JKS;
    private static final int HTTP_80 = 80;

    private String realmBasePath = "realms";
    private final String realm;
    private String trustStoreName;

    private boolean runKeycloakInProdMode = false;

    /**
     * KeycloakService constructor, supported since Keycloak 18.
     *
     * @param realmFile for example /test-realm-realm.json
     * @param realmName
     * @param realmBasePath such as "/realms" used by Keycloak 18 or "auth/realms" used by previous versions
     * @param runKeycloakInProdMode the prod mode needs to set up certificate and use https protocol
     * @param keystoreFormat the format which should be used to enable TLS on Keycloak
     */
    public KeycloakService(String realmFile, String realmName, String realmBasePath, boolean runKeycloakInProdMode,
            Format keystoreFormat) {
        this(realmName);
        this.realmBasePath = normalizeRealmBasePath(realmBasePath);
        this.runKeycloakInProdMode = runKeycloakInProdMode;
        withProperty("KEYCLOAK_REALM_IMPORT", RESOURCE_WITH_DESTINATION_PREFIX + REALM_DEST_PATH + "|" + realmFile);
        if (runKeycloakInProdMode) {
            prepareKeycloakToStartInProdMode(keystoreFormat);
        }
    }

    /**
     * KeycloakService constructor, supported since Keycloak 18.
     *
     * @param realmFile for example /test-realm-realm.json
     * @param realmName
     * @param realmBasePath such as "/realms" used by Keycloak 18 or "auth/realms" used by previous versions
     * @param runKeycloakInProdMode the prod mode needs to set up certificate and use https protocol
     */
    public KeycloakService(String realmFile, String realmName, String realmBasePath, boolean runKeycloakInProdMode) {
        this(realmFile, realmName, realmBasePath, runKeycloakInProdMode, DEFAULT_KEYSTORE_FORMAT);
    }

    /**
     * KeycloakService constructor, supported since Keycloak 18.
     *
     * @param realmFile for example /test-realm-realm.json
     * @param realmName
     * @param realmBasePath such as "/realms" used by Keycloak 18 or "auth/realms" used by previous versions
     */
    public KeycloakService(String realmFile, String realmName, String realmBasePath) {
        this(realmFile, realmName, realmBasePath, false);
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
            TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
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

    private void prepareKeycloakToStartInProdMode(Format keystoreFormat) {
        try {
            CertificateRequest request = new CertificateRequest()
                    .withName(KEYSTORE_PREFIX)
                    .withPassword(KEYSTORE_PASSWORD)
                    .withSubjectAlternativeName(getSubjectAlternativeName())
                    .withFormat(keystoreFormat);
            List<CertificateFiles> certificateFiles = new CertificateGenerator(Path.of("target", "test-classes"),
                    true)
                    .generate(request);
            setPathsAndPropertiesForDifferentFormats(certificateFiles, keystoreFormat);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void setPathsAndPropertiesForDifferentFormats(List<CertificateFiles> certificateFiles, Format keystoreFormat)
            throws IOException {
        if (keystoreFormat.equals(Format.PKCS12)) {
            String keystoreName = ((Pkcs12CertificateFiles) certificateFiles.get(0)).keyStoreFile().getFileName().toString();
            setupTruststoreName(((Pkcs12CertificateFiles) certificateFiles.get(0)).trustStoreFile().toAbsolutePath());

            withProperty("KC_HTTPS_KEY_STORE_FILE", SECRET_WITH_DESTINATION_PREFIX + KEYSTORE_DEST_PATH + "|" + keystoreName);
            withProperty("KC_HTTPS_KEY_STORE_PASSWORD", KEYSTORE_PASSWORD);
        } else if (keystoreFormat.equals(Format.JKS)) {
            String keystoreName = ((JksCertificateFiles) certificateFiles.get(0)).keyStoreFile().getFileName().toString();
            setupTruststoreName(((JksCertificateFiles) certificateFiles.get(0)).trustStoreFile().toAbsolutePath());

            withProperty("KC_HTTPS_KEY_STORE_FILE", SECRET_WITH_DESTINATION_PREFIX + KEYSTORE_DEST_PATH + "|" + keystoreName);
            withProperty("KC_HTTPS_KEY_STORE_PASSWORD", KEYSTORE_PASSWORD);
        } else if (keystoreFormat.equals(Format.PEM)) {
            String certFile = ((PemCertificateFiles) certificateFiles.get(0)).certFile().getFileName().toString();
            String certKeyFile = ((PemCertificateFiles) certificateFiles.get(0)).keyFile().getFileName().toString();

            setupTruststoreName(((PemCertificateFiles) certificateFiles.get(0)).trustFile().toAbsolutePath());

            withProperty("KC_HTTPS_CERTIFICATE_FILE", SECRET_WITH_DESTINATION_PREFIX + KEYSTORE_DEST_PATH + "|" + certFile);
            withProperty("KC_HTTPS_CERTIFICATE_KEY_FILE", SECRET_WITH_DESTINATION_PREFIX + KEYSTORE_DEST_PATH + "|"
                    + certKeyFile);
        } else {
            throw new IllegalArgumentException("Unsupported keystore format.");
        }
    }

    private void setupTruststoreName(Path trustStorePath) throws IOException {
        if (isBareMetalPlatform()) {
            trustStoreName = trustStorePath.toAbsolutePath().toString();
        } else {
            trustStoreName = trustStorePath.getFileName().toString();
            //Need to copy the truststore to these location to enable OpenShift Quarkus scenarios work without complex mounting
            Files.copy(trustStorePath,
                    Path.of("target", "classes", trustStorePath.getFileName().toString()).toAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(trustStorePath,
                    Path.of("src", "main", "resources", trustStorePath.getFileName().toString()).toAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public String getTrustStoreName() {
        return trustStoreName;
    }

    public String getSubjectAlternativeName() {
        if (getOpenShiftUrl() == null) {
            return "localhost";
        }

        return getOpenShiftUrl().getHost().replace("api.", "*.apps.");
    }
}
