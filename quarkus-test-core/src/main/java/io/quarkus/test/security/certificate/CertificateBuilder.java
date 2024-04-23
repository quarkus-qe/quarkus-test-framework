package io.quarkus.test.security.certificate;

import static io.quarkus.test.utils.PropertiesUtils.DESTINATION_TO_FILENAME_SEPARATOR;
import static io.quarkus.test.utils.PropertiesUtils.SECRET_WITH_DESTINATION_PREFIX;
import static io.quarkus.test.utils.TestExecutionProperties.isKubernetesPlatform;
import static io.quarkus.test.utils.TestExecutionProperties.isOpenshiftPlatform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.quarkus.test.services.Certificate.Format;
import io.quarkus.test.utils.FileUtils;
import me.escoffier.certs.CertificateGenerator;
import me.escoffier.certs.CertificateRequest;
import me.escoffier.certs.JksCertificateFiles;
import me.escoffier.certs.PemCertificateFiles;
import me.escoffier.certs.Pkcs12CertificateFiles;

public interface CertificateBuilder {

    /**
     * Test context instance key.
     */
    String INSTANCE_KEY = "io.quarkus.test.security.certificate#INSTANCE";

    List<Certificate> certificates();

    interface Certificate {

        String keystorePath();

        String truststorePath();

        Map<String, String> configProperties();

    }

    static CertificateBuilder of(io.quarkus.test.services.Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            return null;
        }
        return createBuilder(certificates);
    }

    static Certificate of(String prefix, Format format, String password) {
        return of(prefix, format, password, false, false);
    }

    private static Certificate of(String prefix, Format format, String password, boolean keystoreProps,
            boolean truststoreProps) {
        Path certsDir = createCertsTempDir(prefix);
        CertificateGenerator generator = new CertificateGenerator(certsDir, true);
        CertificateRequest request = (new CertificateRequest()).withName(prefix)
                .withClientCertificate(false)
                .withFormat(me.escoffier.certs.Format.valueOf(format.toString()))
                .withCN("localhost")
                .withPassword(password)
                .withDuration(Duration.ofDays(2));
        String trustStoreLocation = null;
        String keyStoreLocation = null;
        try {
            var certFile = generator.generate(request).get(0);
            if (certFile instanceof Pkcs12CertificateFiles pkcs12CertFile) {
                if (pkcs12CertFile.trustStoreFile() != null) {
                    trustStoreLocation = pkcs12CertFile.trustStoreFile().toAbsolutePath().toString();
                }
                if (pkcs12CertFile.keyStoreFile() != null) {
                    keyStoreLocation = pkcs12CertFile.keyStoreFile().toAbsolutePath().toString();
                }
            } else if (certFile instanceof PemCertificateFiles pemCertsFile) {
                if (pemCertsFile.serverTrustFile() != null) {
                    trustStoreLocation = pemCertsFile.serverTrustFile().toAbsolutePath().toString();
                }
            } else if (certFile instanceof JksCertificateFiles jksCertFile) {
                if (jksCertFile.trustStoreFile() != null) {
                    trustStoreLocation = jksCertFile.trustStoreFile().toAbsolutePath().toString();
                }
                if (jksCertFile.keyStoreFile() != null) {
                    keyStoreLocation = jksCertFile.keyStoreFile().toAbsolutePath().toString();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate", e);
        }
        Map<String, String> props = new HashMap<>();
        if (trustStoreLocation != null) {
            if (isOpenshiftPlatform() || isKubernetesPlatform()) {
                // mount truststore to the pod
                props.put(getRandomPropKey("truststore"), toSecretProperty(trustStoreLocation));
            }
            if (truststoreProps) {
                props.put("quarkus.http.ssl.certificate.trust-store-file", trustStoreLocation);
                props.put("quarkus.http.ssl.certificate.trust-store-file-type", format.toString());
                props.put("quarkus.http.ssl.certificate.trust-store-password", password);
            }
        }
        if (keyStoreLocation != null) {
            if (isOpenshiftPlatform() || isKubernetesPlatform()) {
                keyStoreLocation = makeFileMountPathUnique(prefix, keyStoreLocation);
                // mount keystore to the pod
                props.put(getRandomPropKey("keystore"), toSecretProperty(keyStoreLocation));
            }
            if (keystoreProps) {
                props.put("quarkus.http.ssl.certificate.key-store-file", keyStoreLocation);
                props.put("quarkus.http.ssl.certificate.key-store-file-type", format.toString());
                props.put("quarkus.http.ssl.certificate.key-store-password", password);
            }
        }
        return new CertificateImpl(keyStoreLocation, trustStoreLocation, Map.copyOf(props));
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

    private static CertificateBuilder createBuilder(io.quarkus.test.services.Certificate[] certificates) {
        Certificate[] generatedCerts = new Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            generatedCerts[i] = of(certificates[i].prefix(), certificates[i].format(), certificates[i].password(),
                    certificates[i].configureKeystore(), certificates[i].configureTruststore());
        }
        return new CertificateBuilderImp(List.of(generatedCerts));
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

    record CertificateBuilderImp(List<Certificate> certificates) implements CertificateBuilder {
    }

    record CertificateImpl(String keystorePath, String truststorePath,
            Map<String, String> configProperties) implements Certificate {
    }
}
