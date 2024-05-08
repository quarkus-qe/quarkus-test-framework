package io.quarkus.test.security.certificate;

record ClientCertificateImpl(String commonName, String keystorePath, String truststorePath) implements ClientCertificate {
}
