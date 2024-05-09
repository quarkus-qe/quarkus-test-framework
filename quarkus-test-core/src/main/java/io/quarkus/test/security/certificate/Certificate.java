package io.quarkus.test.security.certificate;

import static io.quarkus.test.services.Certificate.Format.PKCS12;
import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.TestExecutionProperties.isKubernetesPlatform;
import static io.quarkus.test.utils.TestExecutionProperties.isOpenshiftPlatform;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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

import io.quarkus.test.utils.FileUtils;
import me.escoffier.certs.CertificateGenerator;
import me.escoffier.certs.CertificateRequest;
import me.escoffier.certs.JksCertificateFiles;
import me.escoffier.certs.PemCertificateFiles;
import me.escoffier.certs.Pkcs12CertificateFiles;

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

    static Certificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password) {
        return of(prefix, format, password, false, false, false, new io.quarkus.test.services.Certificate.ClientCertificate[0]);
    }

    static Certificate of(String prefix, io.quarkus.test.services.Certificate.Format format, String password,
            boolean keystoreProps, boolean truststoreProps, boolean keystoreManagementInterfaceProps,
            io.quarkus.test.services.Certificate.ClientCertificate[] clientCertificates) {
        Map<String, String> props = new HashMap<>();
        CertificateGenerator generator = new CertificateGenerator(createCertsTempDir(prefix), false);
        String serverTrustStoreLocation = null;
        String serverKeyStoreLocation = null;
        String keyLocation = null;
        String certLocation = null;
        List<ClientCertificate> generatedClientCerts = new ArrayList<>();
        String[] cnAttrs = collectCommonNames(clientCertificates);
        var unknownClientCn = getUnknownClientCnAttr(clientCertificates, cnAttrs);

        // 1. GENERATE FIRST CERTIFICATE AND SERVER KEYSTORE AND TRUSTSTORE
        boolean withClientCerts = cnAttrs.length > 0;
        String cn = withClientCerts ? cnAttrs[0] : "localhost";
        final CertificateRequest request = createCertificateRequest(prefix, format, password, withClientCerts, cn);
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
                if (isOpenshiftPlatform() || isKubernetesPlatform()) {
                    if (certLocation != null) {
                        certLocation = makeFileMountPathUnique(prefix, certLocation);
                        // mount certificate to the pod
                        props.put(getRandomPropKey("crt"), toSecretProperty(certLocation));
                    }

                    if (keyLocation != null) {
                        keyLocation = makeFileMountPathUnique(prefix, keyLocation);
                        // mount private key to the pod
                        props.put(getRandomPropKey("key"), toSecretProperty(keyLocation));
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
            if (format != PKCS12) {
                throw new IllegalArgumentException(
                        "Generation of more than one client certificate is only implemented for PKCS12.");
            }
            var correctClientTruststore = Path.of(generatedClientCerts.get(0).truststorePath()).toFile();
            for (int i = 1; i < cnAttrs.length; i++) {
                var clientCn = cnAttrs[i];
                var clientPrefix = clientCn + "-" + prefix;
                var clientRequest = createCertificateRequest(clientPrefix, format, password, true, clientCn);
                try {
                    var clientCertFile = (Pkcs12CertificateFiles) generator.generate(clientRequest).get(0);
                    fixGeneratedClientCerts(clientPrefix, password, clientCertFile, correctClientTruststore,
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
            if (isOpenshiftPlatform() || isKubernetesPlatform()) {
                // mount truststore to the pod
                props.put(getRandomPropKey("truststore"), toSecretProperty(serverTrustStoreLocation));
            }
            if (truststoreProps) {
                props.put("quarkus.http.ssl.certificate.trust-store-file", serverTrustStoreLocation);
                props.put("quarkus.http.ssl.certificate.trust-store-file-type", format.toString());
                props.put("quarkus.http.ssl.certificate.trust-store-password", password);
            }
        }
        if (serverKeyStoreLocation != null) {
            if (isOpenshiftPlatform() || isKubernetesPlatform()) {
                serverKeyStoreLocation = makeFileMountPathUnique(prefix, serverKeyStoreLocation);
                // mount keystore to the pod
                props.put(getRandomPropKey("keystore"), toSecretProperty(serverKeyStoreLocation));
            }
            if (keystoreProps) {
                props.put("quarkus.http.ssl.certificate.key-store-file", serverKeyStoreLocation);
                props.put("quarkus.http.ssl.certificate.key-store-file-type", format.toString());
                props.put("quarkus.http.ssl.certificate.key-store-password", password);
            }
            if (keystoreManagementInterfaceProps) {
                props.put("quarkus.management.ssl.certificate.key-store-file", serverKeyStoreLocation);
                props.put("quarkus.management.ssl.certificate.key-store-file-type", format.toString());
                props.put("quarkus.management.ssl.certificate.key-store-password", password);
            }
        }

        return new CertificateImpl(serverKeyStoreLocation, serverTrustStoreLocation, Map.copyOf(props),
                List.copyOf(generatedClientCerts), password, format.toString(), keyLocation, certLocation, prefix);
    }

    private static String getUnknownClientCnAttr(io.quarkus.test.services.Certificate.ClientCertificate[] clientCertificates,
            String[] cnAttrs) {
        var cnAttrsUnknownToServer = Arrays.stream(clientCertificates)
                .filter(io.quarkus.test.services.Certificate.ClientCertificate::unknownToServer)
                .map(io.quarkus.test.services.Certificate.ClientCertificate::cnAttribute)
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

    private static String[] collectCommonNames(io.quarkus.test.services.Certificate.ClientCertificate[] clientCertificates) {
        return Arrays
                .stream(clientCertificates)
                .map(io.quarkus.test.services.Certificate.ClientCertificate::cnAttribute)
                .toArray(String[]::new);
    }

    private static CertificateRequest createCertificateRequest(String prefix,
            io.quarkus.test.services.Certificate.Format format, String password, boolean withClientCerts, String cn) {
        return (new CertificateRequest())
                .withName(prefix)
                .withFormat(me.escoffier.certs.Format.valueOf(format.toString()))
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
        int fileNameSeparatorIdx = path.lastIndexOf(File.separator);
        String fileName = path.substring(fileNameSeparatorIdx + 1);
        String pathToFile = path.substring(0, fileNameSeparatorIdx);
        return SECRET_WITH_DESTINATION_PREFIX + pathToFile + DESTINATION_TO_FILENAME_SEPARATOR + fileName;
    }

    private static String getPathOrNull(Path file) {
        if (file != null) {
            return file.toAbsolutePath().toString();
        }
        return null;
    }

    private static String makeFileMountPathUnique(String prefix, String storeLocation) {
        var newTempCertDir = createCertsTempDir(prefix);
        var storeFile = Path.of(storeLocation).toFile();
        FileUtils.copyFileTo(storeFile, newTempCertDir);
        return newTempCertDir.resolve(storeFile.getName()).toAbsolutePath().toString();
    }

    private static Path createCertsTempDir(String prefix) {
        Path certsDir;
        try {
            certsDir = Files.createTempDirectory(prefix + "-certs");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return certsDir;
    }
}
