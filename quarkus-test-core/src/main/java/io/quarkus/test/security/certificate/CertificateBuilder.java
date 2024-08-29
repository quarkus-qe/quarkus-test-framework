package io.quarkus.test.security.certificate;

import static io.quarkus.test.security.certificate.Certificate.createCertsTempDir;

import java.util.Arrays;
import java.util.List;

public interface CertificateBuilder {

    /**
     * Test context instance key.
     */
    String INSTANCE_KEY = "io.quarkus.test.security.certificate#INSTANCE";

    List<Certificate> certificates();

    Certificate findCertificateByPrefix(String prefix);

    /**
     * Regenerates certificate with {@code prefix}.
     * The new certificate will be stored at the same location as the original one.
     * All generated files will have same name, certificate attributes, password etc.
     */
    Certificate regenerateCertificate(String prefix, CertificateRequestCustomizer... customizers);

    static CertificateBuilder of(Certificate certificate) {
        return new CertificateBuilderImp(List.of(certificate));
    }

    static CertificateBuilder of(io.quarkus.test.services.Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            return null;
        }
        return createBuilder(certificates);
    }

    private static CertificateBuilder createBuilder(io.quarkus.test.services.Certificate[] certificates) {
        Certificate[] generatedCerts = new Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            var cert = certificates[i];
            var clientCertReqs = Arrays.stream(cert.clientCertificates())
                    .map(cc -> new ClientCertificateRequest(cc.cnAttribute(), cc.unknownToServer()))
                    .toArray(ClientCertificateRequest[]::new);
            generatedCerts[i] = Certificate.ofInterchangeable(new CertificateOptions(cert.prefix(), cert.format(),
                    cert.password(), cert.configureKeystore(), cert.configureTruststore(),
                    cert.configureManagementInterface(),
                    clientCertReqs, createCertsTempDir(cert.prefix()), new DefaultContainerMountStrategy(cert.prefix()),
                    false, null, null, null, null, cert.useTlsRegistry(), cert.tlsConfigName(), cert.configureHttpServer()));
        }
        return new CertificateBuilderImp(List.of(generatedCerts));
    }
}
