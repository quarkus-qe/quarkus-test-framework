package io.quarkus.test.security.certificate;

public interface ClientCertificate {

    String keystorePath();

    String truststorePath();

    String commonName();

}
