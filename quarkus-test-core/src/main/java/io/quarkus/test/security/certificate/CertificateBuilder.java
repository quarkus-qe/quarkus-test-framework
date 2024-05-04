package io.quarkus.test.security.certificate;

import java.util.List;

public interface CertificateBuilder {

    /**
     * Test context instance key.
     */
    String INSTANCE_KEY = "io.quarkus.test.security.certificate#INSTANCE";

    List<Certificate> certificates();

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
            generatedCerts[i] = Certificate.of(cert.prefix(), cert.format(), cert.password(), cert.configureKeystore(),
                    cert.configureTruststore(), cert.configureKeystoreForManagementInterface(), cert.clientCertificates());
        }
        return new CertificateBuilderImp(List.of(generatedCerts));
    }
}
