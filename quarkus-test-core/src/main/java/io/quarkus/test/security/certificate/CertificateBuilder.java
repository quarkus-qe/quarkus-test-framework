package io.quarkus.test.security.certificate;

import static io.quarkus.test.security.certificate.Certificate.createCertsTempDir;
import static io.quarkus.test.services.Certificate.DEFAULT_CONFIG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.test.utils.TestExecutionProperties;

public interface CertificateBuilder {

    /**
     * Test context instance key.
     */
    String INSTANCE_KEY = "io.quarkus.test.security.certificate#INSTANCE";

    List<Certificate> certificates();

    Certificate findCertificateByPrefix(String prefix);

    ServingCertificateConfig servingCertificateConfig();

    /**
     * Regenerates certificate with {@code prefix}.
     * The new certificate will be stored at the same location as the original one.
     * All generated files will have same name, certificate attributes, password etc.
     */
    Certificate regenerateCertificate(String prefix, CertificateRequestCustomizer... customizers);

    static CertificateBuilder of(Certificate certificate) {
        return new CertificateBuilderImpl(List.of(certificate), null);
    }

    static CertificateBuilder of(io.quarkus.test.services.Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            return null;
        }
        return createBuilder(certificates);
    }

    private static CertificateBuilder createBuilder(io.quarkus.test.services.Certificate[] certificates) {
        var svcCertConfigBuilder = ServingCertificateConfig.builder();
        List<Certificate> generatedCerts = new ArrayList<>();
        for (int i = 0; i < certificates.length; i++) {
            var cert = certificates[i];
            configureServingCertificates(cert, svcCertConfigBuilder);
            boolean generateCerts = cert.configureHttpServer() || cert.configureManagementInterface()
                    || cert.configureKeystore() || cert.configureTruststore() || cert.clientCertificates().length > 0;
            if (!generateCerts) {
                continue;
            }
            var clientCertReqs = Arrays.stream(cert.clientCertificates())
                    .map(cc -> new ClientCertificateRequest(cc.cnAttribute(), cc.unknownToServer()))
                    .toArray(ClientCertificateRequest[]::new);
            generatedCerts.add(Certificate.ofInterchangeable(new CertificateOptions(cert.prefix(), cert.format(),
                    cert.password(), cert.configureKeystore(), cert.configureTruststore(),
                    cert.configureManagementInterface(),
                    clientCertReqs, createCertsTempDir(cert.prefix()), new DefaultContainerMountStrategy(cert.prefix()),
                    false, null, null, null, null, cert.useTlsRegistry(), cert.tlsConfigName(), cert.configureHttpServer())));
        }
        return new CertificateBuilderImpl(List.copyOf(generatedCerts), svcCertConfigBuilder.build());
    }

    private static void configureServingCertificates(io.quarkus.test.services.Certificate cert,
            ServingCertificateConfig.ServingCertificateConfigBuilder svcCertConfigBuilder) {
        if (TestExecutionProperties.isOpenshiftPlatform() && cert.useTlsRegistry()) {
            boolean servingCertificatesEnabled = cert.servingCertificates().length > 0;
            if (servingCertificatesEnabled) {
                for (var servingCertificate : cert.servingCertificates()) {
                    if (servingCertificate.addServiceCertificate()) {
                        svcCertConfigBuilder.withAddServiceCertificate(true);
                    }
                    if (servingCertificate.injectCABundle()) {
                        svcCertConfigBuilder.withInjectCABundle(true);
                    }
                    if (servingCertificate.useKeyStoreProvider()) {
                        svcCertConfigBuilder.withUseKeyStoreProvider(true);
                    }
                }
                if (!DEFAULT_CONFIG.equals(cert.tlsConfigName())) {
                    svcCertConfigBuilder.withTlsConfigName(cert.tlsConfigName());
                }
            }
        }
    }
}
