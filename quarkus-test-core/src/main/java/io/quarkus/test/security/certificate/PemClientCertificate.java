package io.quarkus.test.security.certificate;

public interface PemClientCertificate extends ClientCertificate {

    String keyPath();

    String certPath();

}
