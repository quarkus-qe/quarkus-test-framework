package io.quarkus.test.security.certificate;

import static io.quarkus.test.services.Certificate.Format.PEM;
import static io.quarkus.test.services.Certificate.Format.PKCS12;
import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.TestExecutionProperties;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;
import io.smallrye.certs.JksCertificateFiles;
import io.smallrye.certs.PemCertificateFiles;
import io.smallrye.certs.Pkcs12CertificateFiles;

public interface Certificate {

    String prefix();

    String format();

    String password();

    String keystorePath();

    String truststorePath();

    Map<String, String> configProperties();

    ClientCertificate getClientCertificateByCn(String cn);

    Collection<ClientCertificate> clientCertificates();

    interface PemCertificate extends Certificate {

        String keyPath();

        String certPath();

    }

    static Certificate.PemCertificate ofRegeneratedCert(CertificateOptions o) {
        return of(o);
    }

    static Certificate.PemCertificate ofInterchangeable(CertificateOptions options) {
        // this allows to swap cert when it is regenerated without making all the classes above mutable
        // all but regenerated certificates should be interchangeable
        // for regenerated certs, it would just add unnecessary layer (but cause no issues)
        return InterchangeableCertificate.wrapCert(of(options), options);
    }

    static Certificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password, Path targetDir,
            ContainerMountStrategy containerMountStrategy, boolean createPkcs12TsForPem) {
        return ofInterchangeable(new CertificateOptions(prefix, format, password, false, false, false,
                new ClientCertificateRequest[0], targetDir, containerMountStrategy, createPkcs12TsForPem, null, null, null,
                null, false, null, false));
    }

    static Certificate.PemCertificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password,
            boolean tlsRegistryEnabled, String tlsConfigName, ClientCertificateRequest[] clientCertRequests) {
        return ofInterchangeable(new CertificateOptions(prefix, format, password, false, false, false,
                clientCertRequests, createCertsTempDir(prefix), new DefaultContainerMountStrategy(prefix), false,
                null, null, null, null, tlsRegistryEnabled, tlsConfigName, false));
    }

    static Certificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password,
            boolean tlsRegistryEnabled, String tlsConfigName) {
        return ofInterchangeable(new CertificateOptions(prefix, format, password, false, false, false,
                new ClientCertificateRequest[0], createCertsTempDir(prefix), new DefaultContainerMountStrategy(prefix), false,
                null, null, null, null, tlsRegistryEnabled, tlsConfigName, false));
    }

    static Certificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password) {
        return of(prefix, format, password, createCertsTempDir(prefix), new DefaultContainerMountStrategy(prefix), false);
    }

    private static Certificate.PemCertificate of(CertificateOptions o) {
        Map<String, String> props = new HashMap<>();
        CertificateGenerator generator = new CertificateGenerator(o.localTargetDir(), true);
        String serverTrustStoreLocation = null;
        String serverKeyStoreLocation = null;
        String keyLocation = null;
        String certLocation = null;
        List<ClientCertificate> generatedClientCerts = new ArrayList<>();
        String[] cnAttrs = collectCommonNames(o.clientCertificates());
        var unknownClientCn = getUnknownClientCnAttr(o.clientCertificates(), cnAttrs);

        // 1. GENERATE FIRST CERTIFICATE AND SERVER KEYSTORE AND TRUSTSTORE
        boolean withClientCerts = cnAttrs.length > 0;
        String cn = withClientCerts ? cnAttrs[0] : "localhost";
        final CertificateRequest request = createCertificateRequest(o.prefix(), o.format(), o.password(), withClientCerts, cn);
        try {
            var certFile = generator.generate(request).get(0);
            if (certFile instanceof Pkcs12CertificateFiles pkcs12CertFile) {
                serverKeyStoreLocation = getPathOrNull(pkcs12CertFile.keyStoreFile());
                if (withClientCerts) {
                    serverTrustStoreLocation = getPathOrNull(pkcs12CertFile.serverTrustStoreFile());
                    var clientKeyStoreLocation = getPathOrNull(pkcs12CertFile.clientKeyStoreFile());
                    var clientTrustStoreLocation = getPathOrNull(pkcs12CertFile.trustStoreFile());
                    generatedClientCerts.add(new ClientCertificateImpl(cn, clientKeyStoreLocation, clientTrustStoreLocation));
                } else {
                    serverTrustStoreLocation = getPathOrNull(pkcs12CertFile.trustStoreFile());
                }
            } else if (certFile instanceof PemCertificateFiles pemCertsFile) {
                keyLocation = getPathOrNull(pemCertsFile.keyFile());
                certLocation = getPathOrNull(pemCertsFile.certFile());
                if (o.createPkcs12TsForPem()) {
                    // PKCS12 truststore
                    serverTrustStoreLocation = createPkcs12TruststoreForPem(pemCertsFile.trustStore(), o.password(), cn);
                } else {
                    if (withClientCerts) {
                        serverTrustStoreLocation = getPathOrNull(pemCertsFile.serverTrustFile());
                        var clientCertLocation = getPathOrNull(pemCertsFile.clientCertFile());
                        var clientKeyLocation = getPathOrNull(pemCertsFile.clientKeyFile());
                        var clientTrustStore = getPathOrNull(pemCertsFile.trustFile());
                        generatedClientCerts
                                .add(new ClientCertificateImpl(cn, null, clientTrustStore, clientKeyLocation,
                                        clientCertLocation));
                    } else {
                        // ca-cert
                        serverTrustStoreLocation = getPathOrNull(pemCertsFile.trustStore());
                    }
                }
                if (o.containerMountStrategy().mountToContainer()) {
                    if (certLocation != null) {
                        var containerMountPath = o.containerMountStrategy().certPath(certLocation);
                        if (o.containerMountStrategy().containerShareMountPathWithApp()) {
                            certLocation = containerMountPath;
                        }

                        // mount certificate to the container
                        props.put(getRandomPropKey("crt"), toSecretProperty(containerMountPath));
                    }

                    if (keyLocation != null) {
                        var containerMountPath = o.containerMountStrategy().keyPath(keyLocation);
                        if (o.containerMountStrategy().containerShareMountPathWithApp()) {
                            keyLocation = containerMountPath;
                        }

                        // mount private key to the container
                        props.put(getRandomPropKey("key"), toSecretProperty(containerMountPath));
                    }
                }
            } else if (certFile instanceof JksCertificateFiles jksCertFile) {
                serverKeyStoreLocation = getPathOrNull(jksCertFile.keyStoreFile());
                if (withClientCerts) {
                    serverTrustStoreLocation = getPathOrNull(jksCertFile.serverTrustStoreFile());
                    var clientKeyStoreLocation = getPathOrNull(jksCertFile.clientKeyStoreFile());
                    var clientTrustStoreLocation = getPathOrNull(jksCertFile.trustStoreFile());
                    generatedClientCerts.add(new ClientCertificateImpl(cn, clientKeyStoreLocation, clientTrustStoreLocation));
                } else {
                    serverTrustStoreLocation = getPathOrNull(jksCertFile.trustStoreFile());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate", e);
        }

        // 2. IF THERE IS MORE THAN ONE CLIENT CERTIFICATE, GENERATE OTHERS
        if (withClientCerts && cnAttrs.length > 1) {
            if (o.format() != PKCS12) {
                throw new IllegalArgumentException(
                        "Generation of more than one client certificate is only implemented for PKCS12.");
            }
            var correctClientTruststore = Path.of(generatedClientCerts.get(0).truststorePath()).toFile();
            for (int i = 1; i < cnAttrs.length; i++) {
                var clientCn = cnAttrs[i];
                var clientPrefix = clientCn + "-" + o.prefix();
                var clientRequest = createCertificateRequest(clientPrefix, o.format(), o.password(), true, clientCn);
                try {
                    var clientCertFile = (Pkcs12CertificateFiles) generator.generate(clientRequest).get(0);
                    fixGeneratedClientCerts(clientPrefix, o.password(), clientCertFile, correctClientTruststore,
                            serverTrustStoreLocation, unknownClientCn, clientCn);
                    generatedClientCerts
                            .add(new ClientCertificateImpl(clientCn, getPathOrNull(clientCertFile.clientKeyStoreFile()),
                                    getPathOrNull(clientCertFile.trustStoreFile())));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // 3. PREPARE QUARKUS APPLICATION CONFIGURATION PROPERTIES
        if (serverTrustStoreLocation != null) {
            if (o.containerMountStrategy().mountToContainer()) {
                var containerMountPath = o.containerMountStrategy().truststorePath(serverTrustStoreLocation);
                if (o.containerMountStrategy().containerShareMountPathWithApp()) {
                    serverTrustStoreLocation = containerMountPath;
                }

                // mount truststore to the container
                props.put(getRandomPropKey("truststore"), toSecretProperty(containerMountPath));
            }
            configureServerTrustStoreProps(o, props, serverTrustStoreLocation);
        }
        if (serverKeyStoreLocation != null) {
            if (o.containerMountStrategy().mountToContainer()) {
                var containerMountPath = o.containerMountStrategy().keystorePath(serverKeyStoreLocation);
                if (o.containerMountStrategy().containerShareMountPathWithApp()) {
                    serverKeyStoreLocation = containerMountPath;
                }

                // mount keystore to the container
                props.put(getRandomPropKey("keystore"), toSecretProperty(containerMountPath));
            }
            configureServerKeyStoreProps(o, props, serverKeyStoreLocation);
        }
        configureManagementInterfaceProps(o, props, serverKeyStoreLocation);
        configureHttpServerProps(o, props);
        configurePemConfigurationProperties(o, props, keyLocation, certLocation, serverTrustStoreLocation);
        doubleBackSlashesOnWin(props);

        return createCertificate(serverKeyStoreLocation, serverTrustStoreLocation, Map.copyOf(props),
                List.copyOf(generatedClientCerts), keyLocation, certLocation, o);
    }

    private static void doubleBackSlashesOnWin(Map<String, String> props) {
        if (OS.WINDOWS.isCurrentOs()) {
            // we need to quote back slashes passed as command lines in Windows as they have special meaning
            // TODO: this must be done for all config properties, but I do not dare to change it now
            //   now is not the good time to break test suite as this PR is going to be backported and we have pressing
            //   matters; let's do it later: https://github.com/quarkus-qe/quarkus-test-framework/issues/1275
            props.replaceAll((key, value) -> value.replace("\\", "\\\\"));
        }
    }

    private static void configurePemConfigurationProperties(CertificateOptions options, Map<String, String> props,
            String keyLocation, String certLocation, String serverTrustStoreLocation) {
        if (options.format() == PEM && options.tlsRegistryEnabled()) {
            var keyStorePropertyPrefix = tlsConfigPropPrefix(options, "key-store");
            if (keyLocation != null) {
                props.put(keyStorePropertyPrefix + "pem-1.key", keyLocation);
            }
            if (certLocation != null) {
                props.put(keyStorePropertyPrefix + "pem-1.cert", certLocation);
            }
            var trustStorePropertyPrefix = tlsConfigPropPrefix(options, "trust-store");
            if (serverTrustStoreLocation != null) {
                props.put(trustStorePropertyPrefix + "certs", serverTrustStoreLocation);
            }
        }
    }

    private static void configureManagementInterfaceProps(CertificateOptions o, Map<String, String> props,
            String serverKeyStoreLocation) {
        if (o.configureManagementInterface()) {
            props.put(TestExecutionProperties.MANAGEMENT_INTERFACE_ENABLED, Boolean.TRUE.toString());
            if (o.tlsRegistryEnabled()) {
                if (isNotDefaultTlsConfig(o)) {
                    // default TLS registry is configured automatically
                    props.put("quarkus.management.tls-configuration-name", o.tlsConfigName());
                }
            } else if (serverKeyStoreLocation != null) {
                props.put("quarkus.management.ssl.certificate.key-store-file", serverKeyStoreLocation);
                props.put("quarkus.management.ssl.certificate.key-store-file-type", o.format().toString());
                props.put("quarkus.management.ssl.certificate.key-store-password", o.password());
            }
        }
    }

    private static void configureHttpServerProps(CertificateOptions o, Map<String, String> props) {
        if (o.configureHttpServer() && o.tlsRegistryEnabled()) {
            if (isNotDefaultTlsConfig(o)) {
                // default TLS registry is configured automatically
                props.put("quarkus.http.tls-configuration-name", o.tlsConfigName());
            }
        }
    }

    private static boolean isNotDefaultTlsConfig(CertificateOptions o) {
        if (o.tlsConfigName() == null) {
            throw new IllegalArgumentException("TLS registry is enabled but TLS config name is null");
        }
        return !io.quarkus.test.services.Certificate.DEFAULT_CONFIG.equals(o.tlsConfigName());
    }

    private static void configureServerKeyStoreProps(CertificateOptions o, Map<String, String> props,
            String serverKeyStoreLocation) {
        if (o.keystoreProps()) {
            if (o.tlsRegistryEnabled()) {
                if (o.format() != PEM) {
                    var propPrefix = tlsConfigPropPrefix(o, "key-store");
                    props.put(propPrefix + "path", serverKeyStoreLocation);
                    props.put(propPrefix + "password", o.password());
                }
            } else {
                props.put("quarkus.http.ssl.certificate.key-store-file", serverKeyStoreLocation);
                props.put("quarkus.http.ssl.certificate.key-store-file-type", o.format().toString());
                props.put("quarkus.http.ssl.certificate.key-store-password", o.password());
            }
        }
    }

    private static void configureServerTrustStoreProps(CertificateOptions o, Map<String, String> props,
            String serverTrustStoreLocation) {
        if (o.truststoreProps()) {
            if (o.tlsRegistryEnabled()) {
                if (o.format() != PEM) {
                    var propPrefix = tlsConfigPropPrefix(o, "trust-store");
                    props.put(propPrefix + "path", serverTrustStoreLocation);
                    props.put(propPrefix + "password", o.password());
                }
            } else {
                props.put("quarkus.http.ssl.certificate.trust-store-file", serverTrustStoreLocation);
                props.put("quarkus.http.ssl.certificate.trust-store-file-type", o.format().toString());
                props.put("quarkus.http.ssl.certificate.trust-store-password", o.password());
            }
        }
    }

    private static String tlsConfigPropPrefix(CertificateOptions o, String propInfix) {
        if (o.tlsConfigName() == null) {
            throw new IllegalArgumentException("TLS registry is enabled but TLS config name is null");
        }
        final String baseConfigProp;
        if (io.quarkus.test.services.Certificate.DEFAULT_CONFIG.equals(o.tlsConfigName())) {
            // default config
            baseConfigProp = "quarkus.tls.";
        } else {
            // named config
            baseConfigProp = "quarkus.tls." + o.tlsConfigName() + ".";
        }
        final String storeFormat = switch (o.format()) {
            case PKCS12 -> "p12.";
            case JKS -> "jks.";
            default -> "pem.";
        };
        return baseConfigProp + propInfix + "." + storeFormat;
    }

    private static Certificate.PemCertificate createCertificate(String keystorePath, String truststorePath,
            Map<String, String> configProperties, Collection<ClientCertificate> clientCertificates, String keyPath,
            String certPath, CertificateOptions o) {
        var format = o.format().toString();

        // if customizers required new location, let's move file there
        certPath = moveFileIfRequired(o.certLocation(), certPath);
        keyPath = moveFileIfRequired(o.keyLocation(), keyPath);
        keystorePath = moveFileIfRequired(o.serverKeyStoreLocation(), keystorePath);
        truststorePath = moveFileIfRequired(o.serverTrustStoreLocation(), truststorePath);

        return new CertificateImpl(keystorePath, truststorePath, Map.copyOf(configProperties),
                clientCertificates, o.password(), format, keyPath, certPath, o.prefix());
    }

    private static String getUnknownClientCnAttr(ClientCertificateRequest[] clientCertificates,
            String[] cnAttrs) {
        var cnAttrsUnknownToServer = Arrays.stream(clientCertificates)
                .filter(ClientCertificateRequest::unknownToServer)
                .map(ClientCertificateRequest::cnAttribute)
                .collect(Collectors.toSet());
        if (cnAttrsUnknownToServer.isEmpty()) {
            return null;
        }

        if (cnAttrsUnknownToServer.size() > 1) {
            throw new IllegalArgumentException("Only one client certificate can be unknown to the server");
        }
        if (cnAttrs.length == 1) {
            throw new IllegalArgumentException("More than one client certificate must be specified to support unknown one");
        }

        var unknownClientCn = cnAttrsUnknownToServer.stream().findFirst().get();

        // make sure the unknown client is not at the first position as it would be harder to implement
        if (unknownClientCn.equals(cnAttrs[0])) {
            var lastCn = cnAttrs[cnAttrs.length - 1];
            cnAttrs[0] = lastCn;
            cnAttrs[cnAttrs.length - 1] = unknownClientCn;
        }

        return unknownClientCn;
    }

    private static void fixGeneratedClientCerts(String prefix, String password, Pkcs12CertificateFiles clientCertFile,
            File correctClientTruststore, String serverTrustStoreLocation, String unknownClientCn, String clientCn)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // ATM the certificate generator we use does not support generation of more than one client certificate
        // as it prefers aliases, however for backwards compatibility I don't want to enforce aliases

        // basically, here we have client certificates for a different server keystore / truststore
        // we need:
        // 1. fix client trust store by reusing the one client truststore we already have
        var targetClientTruststore = clientCertFile.trustStoreFile().toFile();
        org.apache.commons.io.FileUtils.copyFile(correctClientTruststore, targetClientTruststore, REPLACE_EXISTING);

        if (unknownClientCn == null || !unknownClientCn.equals(clientCn)) {
            // 2. add our client certificate to server truststore
            var clientKs = KeyStore.getInstance(clientCertFile.clientKeyStoreFile().toFile(), password.toCharArray());
            var clientCertFromKs = clientKs.getCertificate(prefix);
            Objects.requireNonNull(clientCertFromKs);
            var serverTsFile = Path.of(serverTrustStoreLocation).toFile();
            var serverTs = KeyStore.getInstance(serverTsFile, password.toCharArray());
            serverTs.setCertificateEntry(prefix, clientCertFromKs);
            try (FileOutputStream trustStoreFos = new FileOutputStream(serverTsFile)) {
                serverTs.store(trustStoreFos, password.toCharArray());
            }
        }
    }

    private static String[] collectCommonNames(ClientCertificateRequest[] clientCertificates) {
        return Arrays
                .stream(clientCertificates)
                .map(ClientCertificateRequest::cnAttribute)
                .toArray(String[]::new);
    }

    private static CertificateRequest createCertificateRequest(String prefix,
            io.quarkus.test.services.Certificate.Format format, String password, boolean withClientCerts, String cn) {
        return (new CertificateRequest())
                .withName(prefix)
                .withFormat(Format.valueOf(format.toString()))
                .withClientCertificate(withClientCerts)
                .withCN(cn)
                .withPassword(password)
                .withSubjectAlternativeName("localhost")
                .withSubjectAlternativeName("0.0.0.0")
                .withDuration(Duration.ofDays(2));
    }

    private static String getRandomPropKey(String store) {
        return store + "-" + new Random().nextInt();
    }

    private static String toSecretProperty(String path) {
        var file = Path.of(path).toFile();
        String fileName = file.getName();
        String pathToFile = file.getParentFile().getAbsolutePath();
        return SECRET_WITH_DESTINATION_PREFIX + pathToFile + DESTINATION_TO_FILENAME_SEPARATOR + fileName;
    }

    private static String getPathOrNull(Path file) {
        if (file != null) {
            return file.toAbsolutePath().toString();
        }
        return null;
    }

    static Path createCertsTempDir(String prefix) {
        Path certsDir;
        try {
            certsDir = Files.createTempDirectory(prefix + "-certs");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return certsDir;
    }

    private static String createPkcs12TruststoreForPem(Path caCertPath, String password, String alias) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            try (FileInputStream is = new FileInputStream(caCertPath.toFile())) {
                X509Certificate cer = (X509Certificate) fact.generateCertificate(is);

                var newTruststorePath = Files.createTempFile("pem-12-truststore", ".p12");

                try (OutputStream truststoreOs = Files.newOutputStream(newTruststorePath)) {
                    KeyStore truststore = KeyStore.getInstance("PKCS12");
                    truststore.load(null, password.toCharArray());
                    truststore.setCertificateEntry(alias, cer);
                    truststore.store(truststoreOs, password.toCharArray());
                }

                return newTruststorePath.toAbsolutePath().toString();
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create PKCS12 truststore", e);
        }
    }

    private static String moveFileIfRequired(String newPath, String currentPath) {
        if (newPath != null) {
            FileUtils.copyFileTo(currentPath, Path.of(newPath));
            return newPath;
        }
        return currentPath;
    }
}
