package io.quarkus.test.security.certificate;

record ClientCertificateImpl(String commonName, String keystorePath, String truststorePath, String keyPath,
        String certPath) implements PemClientCertificate {

    ClientCertificateImpl(String commonName, String keystorePath, String truststorePath) {
        this(commonName, keystorePath, truststorePath, null, null);
    }

}
